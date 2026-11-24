package org.sylvester.engine.service;

import org.sylvester.engine.dto.req.CouponTemplateRemindCancelReqDTO;
import org.sylvester.engine.dto.req.CouponTemplateRemindQueryReqDTO;
import org.sylvester.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import org.sylvester.engine.service.handler.remind.dto.CouponTemplateRemindDTO;

import java.util.List;

/**
 * 优惠券抢券提醒接口
 */
public interface RemindCouponTemplate {

    /**
     * 提醒用户抢券
     *
     * @param remindCouponTemplateDTO 提醒所需要的信息
     */
    boolean remind(CouponTemplateRemindDTO remindCouponTemplateDTO);
}