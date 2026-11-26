package org.sylvester.settlement.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.sylvester.framework.config.RedisDistributedProperties;
import org.sylvester.framework.exception.ClientException;
import org.sylvester.settlement.common.context.UserContext;
import org.sylvester.settlement.dto.req.QueryCouponGoodsReqDTO;
import org.sylvester.settlement.dto.req.QueryCouponsReqDTO;
import org.sylvester.settlement.dto.resp.CouponTemplateQueryRespDTO;
import org.sylvester.settlement.dto.resp.QueryCouponsDetailRespDTO;
import org.sylvester.settlement.dto.resp.QueryCouponsRespDTO;
import org.sylvester.settlement.service.CouponQueryService;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.sylvester.settlement.common.constant.EngineRedisConstant.COUPON_TEMPLATE_KEY;
import static org.sylvester.settlement.common.constant.EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY;

/**
 * 查询用户可用优惠券列表接口
 */
@Service
@RequiredArgsConstructor
public class CouponQueryServiceImpl implements CouponQueryService {

    private final RedisDistributedProperties redisDistributedProperties;
    private final StringRedisTemplate stringRedisTemplate;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Override
    public QueryCouponsRespDTO listQueryUserCoupons(QueryCouponsReqDTO requestParam) {
        // Step 1: 获取 Redis 中的用户优惠券列表
        Set<String> rangeUserCoupons = stringRedisTemplate.opsForZSet().range(
                String.format(USER_COUPON_TEMPLATE_LIST_KEY, UserContext.getUserId()), 0, -1);

        if (rangeUserCoupons == null || rangeUserCoupons.isEmpty()) {
            return QueryCouponsRespDTO.builder()
                    .availableCoupons(new ArrayList<>())
                    .notAvailableCoupons(new ArrayList<>())
                    .build();
        }

        // 构建 Redis Key 列表
        List<String> couponTemplateIds = rangeUserCoupons.stream()
                .map(each -> StrUtil.split(each, "_").get(0))
                .map(this::buildCouponTemplateCacheKey)
                .toList();

        // 同步获取 Redis 数据并进行解析、转换和分区
        List<Object> rawCouponDataList = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            couponTemplateIds.forEach(each -> connection.hashCommands().hGetAll(each.getBytes()));
            return null;
        });

        // 解析 Redis 数据，并按 `goods` 字段进行分区处理
        Map<Boolean, List<CouponTemplateQueryRespDTO>> partitioned = JSON.parseArray(JSON.toJSONString(rawCouponDataList), CouponTemplateQueryRespDTO.class)
                .stream()
                .collect(Collectors.partitioningBy(coupon -> StrUtil.isEmpty(coupon.getGoods())));

        // 拆分后的两个列表
        List<CouponTemplateQueryRespDTO> goodsEmptyList = partitioned.get(true); // goods 为空的列表
        List<CouponTemplateQueryRespDTO> goodsNotEmptyList = partitioned.get(false); // goods 不为空的列表

        // 针对当前订单可用/不可用的优惠券列表
        List<QueryCouponsDetailRespDTO> availableCoupons = Collections.synchronizedList(new ArrayList<>());
        List<QueryCouponsDetailRespDTO> notAvailableCoupons = Collections.synchronizedList(new ArrayList<>());

        // Step 2: 并行处理 goodsEmptyList 和 goodsNotEmptyList
        CompletableFuture<Void> emptyGoodsTask = CompletableFuture.runAsync(() -> {
            processEmptyGoodsCoupons(goodsEmptyList, requestParam, availableCoupons, notAvailableCoupons);
        }, executorService);

        CompletableFuture<Void> notEmptyGoodsTask = CompletableFuture.runAsync(() -> {
            Map<String, QueryCouponGoodsReqDTO> goodsRequestMap = Optional.ofNullable(requestParam.getGoodsList()).orElseGet(ArrayList::new).stream()
                    .collect(Collectors.toMap(QueryCouponGoodsReqDTO::getGoodsNumber, Function.identity()));
            processNonEmptyGoodsCoupons(goodsNotEmptyList, goodsRequestMap, availableCoupons, notAvailableCoupons);
        }, executorService);

        // Step 3: 等待两个异步任务完成
        CompletableFuture.allOf(emptyGoodsTask, notEmptyGoodsTask).join();

        // 与业内标准一致，按最终优惠力度从大到小排序
        availableCoupons.sort((c1, c2) -> c2.getCouponAmount().compareTo(c1.getCouponAmount()));

        // 构建最终结果并返回
        return QueryCouponsRespDTO.builder()
                .availableCoupons(availableCoupons)
                .notAvailableCoupons(notAvailableCoupons)
                .build();
    }

    // 处理空商品列表的优惠券逻辑
    private void processEmptyGoodsCoupons(List<CouponTemplateQueryRespDTO> goodsEmptyList, QueryCouponsReqDTO requestParam,
                                          List<QueryCouponsDetailRespDTO> availableCoupons, List<QueryCouponsDetailRespDTO> notAvailableCoupons) {
        goodsEmptyList.forEach(each -> {
            QueryCouponsDetailRespDTO resultCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
            JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
            handleCouponLogic(resultCouponDetail, jsonObject, requestParam.getOrderAmount(), availableCoupons, notAvailableCoupons);
        });
    }

    // 处理非空商品列表的优惠券逻辑
    private void processNonEmptyGoodsCoupons(List<CouponTemplateQueryRespDTO> goodsNotEmptyList, Map<String, QueryCouponGoodsReqDTO> goodsRequestMap,
                                             List<QueryCouponsDetailRespDTO> availableCoupons, List<QueryCouponsDetailRespDTO> notAvailableCoupons) {
        goodsNotEmptyList.forEach(each -> {
            QueryCouponsDetailRespDTO resultCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
            QueryCouponGoodsReqDTO couponGoods = goodsRequestMap.get(each.getGoods());
            if (couponGoods == null) {
                notAvailableCoupons.add(resultCouponDetail);
            } else {
                JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
                handleCouponLogic(resultCouponDetail, jsonObject, couponGoods.getGoodsAmount(), availableCoupons, notAvailableCoupons);
            }
        });
    }

    // 优惠券判断逻辑，根据条件判断放入可用或不可用列表
    private void handleCouponLogic(QueryCouponsDetailRespDTO resultCouponDetail, JSONObject jsonObject, BigDecimal amount,
                                   List<QueryCouponsDetailRespDTO> availableCoupons, List<QueryCouponsDetailRespDTO> notAvailableCoupons) {
        // 模板缓存可能已过期，此时拿不到消费规则，按不可用处理，避免整个查询报错
        if (jsonObject == null) {
            notAvailableCoupons.add(resultCouponDetail);
            return;
        }
        BigDecimal termsOfUse = jsonObject.getBigDecimal("termsOfUse");
        BigDecimal maximumDiscountAmount = jsonObject.getBigDecimal("maximumDiscountAmount");

        switch (resultCouponDetail.getType()) {
            case 0: // 立减券
                resultCouponDetail.setCouponAmount(maximumDiscountAmount);
                availableCoupons.add(resultCouponDetail);
                break;
            case 1: // 满减券
                if (amount.compareTo(termsOfUse) >= 0) {
                    resultCouponDetail.setCouponAmount(maximumDiscountAmount);
                    availableCoupons.add(resultCouponDetail);
                } else {
                    notAvailableCoupons.add(resultCouponDetail);
                }
                break;
            case 2: // 折扣券
                if (amount.compareTo(termsOfUse) >= 0) {
                    BigDecimal discountRate = jsonObject.getBigDecimal("discountRate");
                    BigDecimal multiply = amount.multiply(discountRate);
                    if (multiply.compareTo(maximumDiscountAmount) >= 0) {
                        resultCouponDetail.setCouponAmount(maximumDiscountAmount);
                    } else {
                        resultCouponDetail.setCouponAmount(multiply);
                    }
                    availableCoupons.add(resultCouponDetail);
                } else {
                    notAvailableCoupons.add(resultCouponDetail);
                }
                break;
            default:
                throw new ClientException("无效的优惠券类型");
        }
    }

    /**
     * 单线程版本，好理解一些。上面的多线程就是基于这个版本演进的
     */
    public QueryCouponsRespDTO listQueryUserCouponsBySync(QueryCouponsReqDTO requestParam) {
        Set<String> rangeUserCoupons = stringRedisTemplate.opsForZSet().range(String.format(USER_COUPON_TEMPLATE_LIST_KEY, UserContext.getUserId()), 0, -1);

        if (rangeUserCoupons == null || rangeUserCoupons.isEmpty()) {
            return QueryCouponsRespDTO.builder()
                    .availableCoupons(new ArrayList<>())
                    .notAvailableCoupons(new ArrayList<>())
                    .build();
        }

        List<String> couponTemplateIds = rangeUserCoupons.stream()
                .map(each -> StrUtil.split(each, "_").get(0))
                .map(this::buildCouponTemplateCacheKey)
                .toList();
        List<Object> couponTemplateList = stringRedisTemplate.executePipelined((RedisCallback<String>) connection -> {
            couponTemplateIds.forEach(each -> connection.hashCommands().hGetAll(each.getBytes()));
            return null;
        });

        List<CouponTemplateQueryRespDTO> couponTemplateDTOList = JSON.parseArray(JSON.toJSONString(couponTemplateList), CouponTemplateQueryRespDTO.class);
        Map<Boolean, List<CouponTemplateQueryRespDTO>> partitioned = couponTemplateDTOList.stream()
                .collect(Collectors.partitioningBy(coupon -> StrUtil.isEmpty(coupon.getGoods())));

        // 拆分后的两个列表
        List<CouponTemplateQueryRespDTO> goodsEmptyList = partitioned.get(true); // goods 为空的列表
        List<CouponTemplateQueryRespDTO> goodsNotEmptyList = partitioned.get(false); // goods 不为空的列表

        // 针对当前订单可用/不可用的优惠券列表
        List<QueryCouponsDetailRespDTO> availableCoupons = new ArrayList<>();
        List<QueryCouponsDetailRespDTO> notAvailableCoupons = new ArrayList<>();

        goodsEmptyList.forEach(each -> {
            JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
            QueryCouponsDetailRespDTO resultQueryCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
            if (jsonObject == null) {
                notAvailableCoupons.add(resultQueryCouponDetail);
                return;
            }
            BigDecimal maximumDiscountAmount = jsonObject.getBigDecimal("maximumDiscountAmount");
            switch (each.getType()) {
                case 0: // 立减券
                    resultQueryCouponDetail.setCouponAmount(maximumDiscountAmount);
                    availableCoupons.add(resultQueryCouponDetail);
                    break;
                case 1: // 满减券
                    // orderAmount 大于或等于 termsOfUse
                    if (requestParam.getOrderAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        resultQueryCouponDetail.setCouponAmount(maximumDiscountAmount);
                        availableCoupons.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCoupons.add(resultQueryCouponDetail);
                    }
                    break;
                case 2: // 折扣券
                    // orderAmount 大于或等于 termsOfUse
                    if (requestParam.getOrderAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        BigDecimal multiply = requestParam.getOrderAmount().multiply(jsonObject.getBigDecimal("discountRate"));
                        if (multiply.compareTo(maximumDiscountAmount) >= 0) {
                            resultQueryCouponDetail.setCouponAmount(maximumDiscountAmount);
                        } else {
                            resultQueryCouponDetail.setCouponAmount(multiply);
                        }
                        availableCoupons.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCoupons.add(resultQueryCouponDetail);
                    }
                    break;
                default:
                    throw new ClientException("无效的优惠券类型");
            }
        });

        Map<String, QueryCouponGoodsReqDTO> goodsRequestMap = Optional.ofNullable(requestParam.getGoodsList()).orElseGet(ArrayList::new).stream()
                .collect(Collectors.toMap(QueryCouponGoodsReqDTO::getGoodsNumber, Function.identity(), (existing, replacement) -> existing));

        goodsNotEmptyList.forEach(each -> {
            QueryCouponGoodsReqDTO couponGoods = goodsRequestMap.get(each.getGoods());
            JSONObject jsonObject = JSON.parseObject(each.getConsumeRule());
            QueryCouponsDetailRespDTO resultQueryCouponDetail = BeanUtil.toBean(each, QueryCouponsDetailRespDTO.class);
            // 订单中不包含该商品或模板缓存缺失时，直接归入不可用，避免空指针
            if (couponGoods == null || jsonObject == null) {
                notAvailableCoupons.add(resultQueryCouponDetail);
                return;
            }
            switch (each.getType()) {
                case 0: // 立减券
                    resultQueryCouponDetail.setCouponAmount(jsonObject.getBigDecimal("maximumDiscountAmount"));
                    availableCoupons.add(resultQueryCouponDetail);
                    break;
                case 1: // 满减券
                    // goodsAmount 大于或等于 termsOfUse
                    if (couponGoods.getGoodsAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        resultQueryCouponDetail.setCouponAmount(jsonObject.getBigDecimal("maximumDiscountAmount"));
                        availableCoupons.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCoupons.add(resultQueryCouponDetail);
                    }
                    break;
                case 2: // 折扣券
                    // goodsAmount 大于或等于 termsOfUse
                    if (couponGoods.getGoodsAmount().compareTo(jsonObject.getBigDecimal("termsOfUse")) >= 0) {
                        BigDecimal discountRate = jsonObject.getBigDecimal("discountRate");
                        BigDecimal multiply = couponGoods.getGoodsAmount().multiply(discountRate);
                        // 折扣金额不能超过最高抵扣金额，与多线程版本保持一致
                        if (multiply.compareTo(jsonObject.getBigDecimal("maximumDiscountAmount")) >= 0) {
                            resultQueryCouponDetail.setCouponAmount(jsonObject.getBigDecimal("maximumDiscountAmount"));
                        } else {
                            resultQueryCouponDetail.setCouponAmount(multiply);
                        }
                        availableCoupons.add(resultQueryCouponDetail);
                    } else {
                        notAvailableCoupons.add(resultQueryCouponDetail);
                    }
                    break;
                default:
                    throw new ClientException("无效的优惠券类型");
            }
        });

        // 与业内标准一致，按最终优惠力度从大到小排序
        availableCoupons.sort((c1, c2) -> c2.getCouponAmount().compareTo(c1.getCouponAmount()));

        return QueryCouponsRespDTO.builder()
                .availableCoupons(availableCoupons)
                .notAvailableCoupons(notAvailableCoupons)
                .build();
    }

    /**
     * 拼装优惠券模板缓存 Key
     * <p>
     * 框架的 key 前缀未配置时为 null，直接拼接会得到 "nullmyone-coupon_engine:template:xxx"，
     * 查不到任何数据，因此这里与前缀默认值保持一致，按空串处理。
     */
    private String buildCouponTemplateCacheKey(String couponTemplateId) {
        String prefix = Optional.ofNullable(redisDistributedProperties.getPrefix()).orElse("");
        return prefix + String.format(COUPON_TEMPLATE_KEY, couponTemplateId);
    }
}