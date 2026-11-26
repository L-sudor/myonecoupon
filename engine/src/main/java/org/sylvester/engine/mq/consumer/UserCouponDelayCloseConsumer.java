package org.sylvester.engine.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.sylvester.engine.common.constant.EngineRedisConstant;
import org.sylvester.engine.common.enums.UserCouponStatusEnum;
import org.sylvester.engine.dao.entity.UserCouponDO;
import org.sylvester.engine.dao.mapper.UserCouponMapper;
import org.sylvester.engine.mq.base.MessageWrapper;
import org.sylvester.engine.mq.event.UserCouponDelayCloseEvent;

/**
 * 用户优惠券延时关闭消费者
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "myone-coupon_engine-service_user-coupon-delay-close_topic${unique-name:}",
        consumerGroup = "myone-coupon_engine-service_user-coupon-delay-close_cg${unique-name:}"
)
@Slf4j(topic = "UserCouponDelayCloseConsumer")
public class UserCouponDelayCloseConsumer implements RocketMQListener<MessageWrapper<UserCouponDelayCloseEvent>> {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserCouponMapper userCouponMapper;

    @Override
    public void onMessage(MessageWrapper<UserCouponDelayCloseEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 延迟关闭用户已领取优惠券 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));
        UserCouponDelayCloseEvent event = messageWrapper.getMessage();

        // 消息体中的编号均为字符串，统一转成 Long 后再参与查询与分片路由
        long userId = Long.parseLong(event.getUserId());
        long userCouponId = Long.parseLong(event.getUserCouponId());
        long couponTemplateId = Long.parseLong(event.getCouponTemplateId());

        // 先修改数据库状态为已过期，再删除缓存：数据库写成功而缓存删除失败时，消息重试仍会重新执行更新（带 status = 未使用条件，重复执行安全）
        UserCouponDO userCouponDO = UserCouponDO.builder()
                .status(UserCouponStatusEnum.EXPIRED.getCode())
                .build();
        LambdaUpdateWrapper<UserCouponDO> updateWrapper = Wrappers.lambdaUpdate(UserCouponDO.class)
                .eq(UserCouponDO::getId, userCouponId)
                .eq(UserCouponDO::getUserId, userId)
                .eq(UserCouponDO::getStatus, UserCouponStatusEnum.UNUSED.getCode())
                .eq(UserCouponDO::getCouponTemplateId, couponTemplateId);
        int updated = userCouponMapper.update(userCouponDO, updateWrapper);
        log.info("[消费者] 延迟关闭用户已领取优惠券 - 更新用户券状态，影响行数：{}", updated);

        // 删除用户领取优惠券模板缓存记录（用户 ID 取自消息体，消费线程没有 Web 请求上下文）
        String userCouponListCacheKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, userId);
        String userCouponItemCacheKey = StrUtil.builder()
                .append(couponTemplateId)
                .append("_")
                .append(userCouponId)
                .toString();
        stringRedisTemplate.opsForZSet().remove(userCouponListCacheKey, userCouponItemCacheKey);
    }
}
