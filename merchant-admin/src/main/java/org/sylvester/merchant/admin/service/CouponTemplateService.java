package org.sylvester.merchant.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.sylvester.merchant.admin.dao.entity.CouponTemplateDO;
import org.sylvester.merchant.admin.dto.req.CouponTemplateSaveReqDTO;

/**
 * 优惠券模板业务逻辑层
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-08
 */
public interface CouponTemplateService extends IService<CouponTemplateDO> {

    /**
     * 创建商家优惠券模板
     *
     * @param requestParam 请求参数
     */
    void createCouponTemplate(CouponTemplateSaveReqDTO requestParam);
}