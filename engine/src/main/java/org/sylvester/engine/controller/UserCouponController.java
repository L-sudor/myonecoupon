package org.sylvester.engine.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.sylvester.engine.dto.req.CouponCreatePaymentReqDTO;
import org.sylvester.engine.dto.req.CouponProcessPaymentReqDTO;
import org.sylvester.engine.dto.req.CouponProcessRefundReqDTO;
import org.sylvester.engine.dto.req.CouponTemplateRedeemReqDTO;
import org.sylvester.engine.service.CouponPayService;
import org.sylvester.engine.service.UserCouponService;
import org.sylvester.framework.result.Result;
import org.sylvester.framework.web.Results;

/**
 * 用户优惠券控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "用户优惠券管理")
public class UserCouponController {

    private final UserCouponService userCouponService;
    private final CouponPayService couponPayService;

    @Operation(summary = "兑换优惠券模板", description = "存在较高流量场景，可类比“秒杀”业务")
    @PostMapping("/api/engine/user-coupon/redeem")
    public Result<Void> redeemUserCoupon(@RequestBody CouponTemplateRedeemReqDTO requestParam) {
        userCouponService.redeemUserCoupon(requestParam);
        return Results.success();
    }

    @Operation(summary = "兑换优惠券模板之消息队列", description = "存在较高流量场景，可类比“秒杀”业务")
    @PostMapping("/api/engine/user-coupon/redeem-mq")
    public Result<Void> redeemUserCouponByMQ(@RequestBody CouponTemplateRedeemReqDTO requestParam) {
        userCouponService.redeemUserCouponByMQ(requestParam);
        return Results.success();
    }

    @Operation(summary = "创建用户优惠券结算单", description = "用户下单时锁定使用的优惠券，一般由订单系统发起调用")
    @PostMapping("/api/engine/user-coupon/create-payment-record")
    public Result<Void> createPaymentRecord(@RequestBody CouponCreatePaymentReqDTO requestParam) {
        couponPayService.createPaymentRecord(requestParam);
        return Results.success();
    }

    @Operation(summary = "核销优惠券结算单", description = "用户支付后核销使用的优惠券，常规来说应该监听支付后的消息队列事件")
    @PostMapping("/api/engine/user-coupon/process-payment")
    public Result<Void> processPayment(@RequestBody CouponProcessPaymentReqDTO requestParam) {
        couponPayService.processPayment(requestParam);
        return Results.success();
    }

    @Operation(summary = "退款优惠券结算单", description = "用户退款成功后返回使用的优惠券，常规来说应该监听退款成功后的消息队列事件")
    @PostMapping("/api/engine/user-coupon/process-refund")
    public Result<Void> processRefund(@RequestBody CouponProcessRefundReqDTO requestParam) {
        couponPayService.processRefund(requestParam);
        return Results.success();
    }
}