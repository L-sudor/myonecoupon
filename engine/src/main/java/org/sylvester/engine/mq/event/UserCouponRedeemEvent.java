package org.sylvester.engine.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sylvester.engine.dto.req.CouponTemplateRedeemReqDTO;
import org.sylvester.engine.dto.resp.CouponTemplateQueryRespDTO;

/**
 * 用户兑换优惠券事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCouponRedeemEvent {

    /**
     * Web 请求参数
     */
    private CouponTemplateRedeemReqDTO requestParam;

    /**
     * 领取次数
     */
    private Integer receiveCount;

    /**
     * 优惠券模板
     */
    private CouponTemplateQueryRespDTO couponTemplate;

    /**
     * 用户 ID
     */
    private String userId;
}