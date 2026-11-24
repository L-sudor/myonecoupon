package org.sylvester.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.sylvester.engine.dao.entity.CouponTemplateRemindDO;
import org.sylvester.engine.dto.req.CouponTemplateRemindCancelReqDTO;
import org.sylvester.engine.dto.req.CouponTemplateRemindCreateReqDTO;
import org.sylvester.engine.dto.req.CouponTemplateRemindQueryReqDTO;
import org.sylvester.engine.dto.resp.CouponTemplateRemindQueryRespDTO;
import org.sylvester.engine.service.handler.remind.dto.CouponTemplateRemindDTO;

import java.util.List;

/**
 * 优惠券预约提醒业务逻辑层
 */
public interface CouponTemplateRemindService extends IService<CouponTemplateRemindDO> {

    /**
     * 创建抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    void createCouponRemind(CouponTemplateRemindCreateReqDTO requestParam);

    /**
     * 分页查询抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    List<CouponTemplateRemindQueryRespDTO> listCouponRemind(CouponTemplateRemindQueryReqDTO requestParam);

    /**
     * 取消抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    void cancelCouponRemind(CouponTemplateRemindCancelReqDTO requestParam);

    /**
     * 检查是否取消抢券预约提醒
     *
     * @param requestParam 请求参数
     */
    boolean isCancelRemind(CouponTemplateRemindDTO requestParam);
}