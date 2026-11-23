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

        // 删除用户领取优惠券模板缓存记录（用户 ID 取自消息体，消费线程没有 Web 请求上下文）
        String userCouponListCacheKey = String.format(EngineRedisConstant.USER_COUPON_TEMPLATE_LIST_KEY, event.getUserId());
        String userCouponItemCacheKey = StrUtil.builder()
                .append(event.getCouponTemplateId())
                .append("_")
                .append(event.getUserCouponId())
                .toString();
        Long removed = stringRedisTemplate.opsForZSet().remove(userCouponListCacheKey, userCouponItemCacheKey);
        if (removed == null || removed == 0L) {
            return;
        }

        // 修改用户领券记录状态为已过期
        UserCouponDO userCouponDO = UserCouponDO.builder()
                .status(UserCouponStatusEnum.EXPIRED.getCode())
                .build();
        LambdaUpdateWrapper<UserCouponDO> updateWrapper = Wrappers.lambdaUpdate(UserCouponDO.class)
                .eq(UserCouponDO::getId, event.getUserCouponId())
                .eq(UserCouponDO::getUserId, event.getUserId())
                .eq(UserCouponDO::getStatus, UserCouponStatusEnum.UNUSED.getCode())
                .eq(UserCouponDO::getCouponTemplateId, event.getCouponTemplateId());
        userCouponMapper.update(userCouponDO, updateWrapper);
    }
}