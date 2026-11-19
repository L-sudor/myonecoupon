package org.sylvester.distribution.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.sylvester.distribution.dao.entity.CouponTemplateDO;

/**
 * 优惠券模板数据库持久层
 */
public interface CouponTemplateMapper extends BaseMapper<CouponTemplateDO> {

    /**
     * 自减优惠券模板库存
     *
     * @param shopNumber       店铺编号
     * @param decrementStock   自减库存数量
     * @param couponTemplateId 优惠券模板 ID
     * @return 是否发生记录变更行数
     */
    int decrementCouponTemplateStock(@Param("shopNumber") Long shopNumber, @Param("couponTemplateId") Long couponTemplateId, @Param("decrementStock") Integer decrementStock);

    /**
     * 自增优惠券模板库存
     *
     * @param shopNumber       店铺编号
     * @param couponTemplateId 优惠券模板 ID
     * @param incrementStock   自增库存数量
     * @return 发生记录变更行数
     */
    int incrementCouponTemplateStock(@Param("shopNumber") Long shopNumber, @Param("couponTemplateId") Long couponTemplateId, @Param("incrementStock") Integer incrementStock);
}