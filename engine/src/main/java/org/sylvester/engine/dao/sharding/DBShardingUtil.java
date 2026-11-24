package org.sylvester.engine.dao.sharding;

import cn.hutool.core.lang.Singleton;

import java.util.Collection;
import java.util.List;

/**
 * 针对项目中 IN 操作跨数据库场景进行拆分数据源
 */
public final class DBShardingUtil {

    /**
     * 优惠券模板分库算法实例。
     * <p>
     * 该实例由 ShardingSphere 解析 shardingsphere-config.yaml 时创建，并在
     * {@link DBHashModShardingAlgorithm#init(java.util.Properties)} 中以 business-tag（coupon-template）为 key 放入 Singleton。
     * 因此这里必须用同一个 key 读取，才能拿到 ShardingSphere 初始化过的那份实例。
     * <p>
     * 注意：不能按类读取。user_coupon_database_mod 使用的是同一个算法类但 sharding-count 不同（32 与 16），
     * 按类名存取会命中同一份实例，导致分库结果与真实写库所在的库不一致。
     */
    private static final String COUPON_TEMPLATE_BUSINESS_TAG = "coupon-template";

    private static final DBHashModShardingAlgorithm COUPON_TEMPLATE_DB_SHARDING_ALGORITHM = Singleton.get(
            COUPON_TEMPLATE_BUSINESS_TAG,
            () -> {
                throw new IllegalStateException("分库算法 [" + COUPON_TEMPLATE_BUSINESS_TAG + "] 尚未初始化，"
                        + "请检查 shardingsphere-config.yaml 中 coupon_template_database_mod 的 business-tag 配置");
            }
    );

    /**
     * 解决查询商家优惠券 IN 场景跨库表不存在问题
     *
     * @param shopNumber 分片键 shopNumber
     * @return 返回 shopNumber 所在的数据源
     */
    public static int doCouponSharding(Long shopNumber) {
        return COUPON_TEMPLATE_DB_SHARDING_ALGORITHM.getShardingMod(shopNumber, getAvailableDatabases().size());
    }

    /**
     * 获取可用的数据源列表
     */
    private static Collection<String> getAvailableDatabases() {
        return List.of("ds0", "ds1");
    }
}