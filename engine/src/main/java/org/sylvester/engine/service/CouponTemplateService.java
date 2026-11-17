package org.sylvester.engine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.sylvester.engine.dao.entity.CouponTemplateDO;
import org.sylvester.engine.dto.req.CouponTemplateQueryReqDTO;
import org.sylvester.engine.dto.resp.CouponTemplateQueryRespDTO;

/**
 * 优惠券模板业务逻辑层
 */
public interface CouponTemplateService extends IService<CouponTemplateDO> {

    /**
     * 查询优惠券模板
     *
     * @param requestParam 请求参数
     * @return 优惠券模板信息
     */
    CouponTemplateQueryRespDTO findCouponTemplate(CouponTemplateQueryReqDTO requestParam);
}