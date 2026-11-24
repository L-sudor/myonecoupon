package org.sylvester.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.sylvester.engine.dao.entity.CouponTemplateRemindDO;
import org.sylvester.engine.dto.req.CouponTemplateRemindCreateReqDTO;

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
}