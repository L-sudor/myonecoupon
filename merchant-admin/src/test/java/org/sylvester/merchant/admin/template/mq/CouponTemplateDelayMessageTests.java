package org.sylvester.merchant.admin.template.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.sylvester.merchant.admin.common.context.UserContext;
import org.sylvester.merchant.admin.common.context.UserInfoDTO;
import org.sylvester.merchant.admin.dao.entity.CouponTemplateDO;
import org.sylvester.merchant.admin.dao.mapper.CouponTemplateMapper;
import org.sylvester.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import org.sylvester.merchant.admin.service.CouponTemplateService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 创建优惠券模板并发送延时关闭消息测试
 * <p>
 * 生效时间取当前时间，有效期 3 分钟，创建成功后会向 RocketMQ 投递一条
 * 3 分钟后才投递的定时消息（走 syncSendDeliverTimeMills）
 */
@Slf4j
@SpringBootTest
public class CouponTemplateDelayMessageTests {

    /**
     * 店铺编号，与 UserConfiguration 中拦截器写入的值保持一致
     */
    private static final Long SHOP_NUMBER = 1810714735922956666L;

    /**
     * 有效期时长：3 分钟
     */
    private static final long VALID_DURATION_MILLIS = 3 * 60 * 1000L;

    private static final int SHARDING_COUNT = 16;

    private static final int DB_COUNT = 2;

    @Autowired
    private CouponTemplateService couponTemplateService;

    @Autowired
    private CouponTemplateMapper couponTemplateMapper;

    @Test
    public void createCouponTemplateWithThreeMinutesValidity() {
        // 直接调用 Service 不会经过 Web 拦截器，需手动写入用户上下文，否则 shopNumber 为空会导致分片路由失败
        UserContext.setUser(new UserInfoDTO("1810518709471555585", "pdd45305558318", SHOP_NUMBER));
        try {
            Date validStartTime = new Date();
            Date validEndTime = new Date(validStartTime.getTime() + VALID_DURATION_MILLIS);
            String couponName = "延时关闭测试券-" + new SimpleDateFormat("HHmmssSSS").format(validStartTime);

            CouponTemplateSaveReqDTO requestParam = new CouponTemplateSaveReqDTO();
            requestParam.setName(couponName);
            requestParam.setSource(0);
            requestParam.setTarget(1);
            requestParam.setGoods("");
            requestParam.setType(0);
            requestParam.setValidStartTime(validStartTime);
            requestParam.setValidEndTime(validEndTime);
            requestParam.setStock(100);
            requestParam.setReceiveRule("{\"limitPerPerson\":1,\"usageInstructions\":\"每次限用一张\"}");
            requestParam.setConsumeRule("{\"termsOfUse\":10,\"maximumDiscountAmount\":3,\"explanationOfUnmetConditions\":\"订单满10元可用\",\"validityPeriod\":\"48\"}");

            log.info("开始创建优惠券模板，名称：{}，生效时间：{}，失效时间：{}",
                    couponName, format(validStartTime), format(validEndTime));

            couponTemplateService.createCouponTemplate(requestParam);

            // 带分片键查询，能查到即代表写入成功
            List<CouponTemplateDO> createdList = couponTemplateMapper.selectList(
                    Wrappers.lambdaQuery(CouponTemplateDO.class)
                            .eq(CouponTemplateDO::getShopNumber, SHOP_NUMBER)
                            .eq(CouponTemplateDO::getName, couponName));

            Assertions.assertFalse(createdList.isEmpty(), "未查询到刚创建的优惠券模板，落库失败");

            CouponTemplateDO couponTemplateDO = createdList.get(0);
            Assertions.assertNotNull(couponTemplateDO.getId(), "模板 ID 未回填，插入可能未生效");

            log.info("创建成功，模板 ID：{}，店铺编号：{}", couponTemplateDO.getId(), couponTemplateDO.getShopNumber());
            log.info("落库位置：{}", predictShardLocation(SHOP_NUMBER));
            log.info("延时关闭消息投递时间：{}（约 3 分钟后触发）", format(couponTemplateDO.getValidEndTime()));
        } finally {
            UserContext.removeUser();
        }
    }

    /**
     * 按分片算法预测落库位置
     * <p>
     * 与 DBHashModShardingAlgorithm / TableHashModShardingAlgorithm 逻辑保持一致：
     * hash = abs(Long.hashCode(shopNumber))，mod = hash % 16，
     * 库下标 = mod / (16 / 2)，表下标 = mod
     */
    private String predictShardLocation(Long shopNumber) {
        int hashCode = (int) (shopNumber ^ (shopNumber >>> 32));
        int mod = (int) (Math.abs((long) hashCode) % SHARDING_COUNT);
        int dbIndex = mod / (SHARDING_COUNT / DB_COUNT);
        return String.format("one_coupon_rebuild_%d.t_coupon_template_%d", dbIndex, mod);
    }

    private String format(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
