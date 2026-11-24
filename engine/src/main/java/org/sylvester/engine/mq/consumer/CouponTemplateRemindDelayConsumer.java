package org.sylvester.engine.mq.consumer;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.sylvester.engine.mq.base.MessageWrapper;
import org.sylvester.engine.mq.event.CouponTemplateRemindDelayEvent;
import org.sylvester.engine.service.CouponTemplateRemindExecutor;
import org.sylvester.engine.service.handler.remind.dto.CouponTemplateRemindDTO;

/**
 * 提醒抢券消费者
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "myone-coupon_engine-service_coupon-remind_topic${unique-name:}",
        consumerGroup = "myone-coupon_engine-service_coupon-remind_cg${unique-name:}"
)
@Slf4j(topic = "CouponTemplateRemindDelayConsumer")
public class CouponTemplateRemindDelayConsumer implements RocketMQListener<MessageWrapper<CouponTemplateRemindDelayEvent>> {

    private final CouponTemplateRemindExecutor couponTemplateRemindExecutor;

    @Override
    public void onMessage(MessageWrapper<CouponTemplateRemindDelayEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 提醒用户抢券 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        CouponTemplateRemindDelayEvent event = messageWrapper.getMessage();
        CouponTemplateRemindDTO couponTemplateRemindDTO = BeanUtil.toBean(event, CouponTemplateRemindDTO.class);

        // 根据不同策略向用户发送消息提醒
        couponTemplateRemindExecutor.executeRemindCouponTemplate(couponTemplateRemindDTO);
    }
}