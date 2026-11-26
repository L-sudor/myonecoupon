package org.sylvester.engine.dao.sharding;

import cn.hutool.core.lang.Singleton;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import org.apache.shardingsphere.infra.util.exception.ShardingSpherePreconditions;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import org.apache.shardingsphere.sharding.exception.algorithm.sharding.ShardingAlgorithmInitializationException;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * 基于 HashMod 方式自定义分库算法
 */
public final class DBHashModShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    @Getter
    private Properties props;

    private int shardingCount;
    private static final String SHARDING_COUNT_KEY = "sharding-count";

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Comparable<?>> shardingValue) {
        long id = toLong(shardingValue.getValue());
        int dbSize = availableTargetNames.size();
        int mod = (int) hashShardingValue(id) % shardingCount / (shardingCount / dbSize);
        int index = 0;
        for (String targetName : availableTargetNames) {
            if (index == mod) {
                return targetName;
            }
            index++;
        }
        throw new IllegalArgumentException("No target found for value: " + id);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Comparable<?>> shardingValue) {
        // 暂无范围分片场景，默认返回空
        return List.of();
    }

    @Override
    public void init(Properties props) {
        this.props = props;
        shardingCount = getShardingCount(props);
        Object businessTag = props.get("business-tag");
        if (Objects.nonNull(businessTag) && StrUtil.isNotBlank(businessTag.toString())) {
            Singleton.put(businessTag.toString(), this);
        }
    }

    private int getShardingCount(final Properties props) {
        ShardingSpherePreconditions.checkState(props.containsKey(SHARDING_COUNT_KEY), () -> new ShardingAlgorithmInitializationException(getType(), "Sharding count cannot be null."));
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    public int getShardingMod(long id, int availableTargetSize) {
        return (int) hashShardingValue(id) % shardingCount / (shardingCount / availableTargetSize);
    }

    /**
     * 分片键在 SQL 中可能是 Long，也可能是字符串形式的数字，统一转成 long 后再参与取模
     */
    private long toLong(final Comparable<?> shardingValue) {
        if (shardingValue instanceof Number number) {
            return number.longValue();
        }
        if (Objects.isNull(shardingValue) || StrUtil.isBlank(shardingValue.toString())) {
            throw new IllegalArgumentException("Sharding value cannot be null or blank.");
        }
        return Long.parseLong(shardingValue.toString().trim());
    }

    private long hashShardingValue(final long id) {
        // 与 Long 的 hashCode 保持一致，保证同一分片键无论以 Long 还是字符串传入，都路由到同一张表
        return Math.abs((long) Long.hashCode(id));
    }
}
