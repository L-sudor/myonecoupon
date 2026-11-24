package org.sylvester.engine.service.impl;

import org.springframework.stereotype.Component;
import org.sylvester.engine.service.RemindCouponTemplate;
import org.sylvester.engine.service.handler.remind.dto.CouponTemplateRemindDTO;

/**
 * 发送邮件的方式提醒用户抢券
 */
@Component
public class SendEmailRemindCouponTemplate implements RemindCouponTemplate {

    /**
     * 以邮件方式提醒用户抢券
     *
     * @param couponTemplateRemindDTO 提醒所需要的信息
     */
    @Override
    public boolean remind(CouponTemplateRemindDTO couponTemplateRemindDTO) {
        // 空实现
        return true;
    }
}