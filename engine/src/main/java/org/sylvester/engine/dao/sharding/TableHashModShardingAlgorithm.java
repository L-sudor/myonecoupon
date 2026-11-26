package org.sylvester.engine.dao.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 基于 HashMod 方式自定义分表算法
 */
public final class TableHashModShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Comparable<?>> shardingValue) {
        long id = toLong(shardingValue.getValue());
        int shardingCount = availableTargetNames.size();
        int mod = (int) hashShardingValue(id) % shardingCount;
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

    /**
     * 分片键在 SQL 中可能是 Long，也可能是字符串形式的数字，统一转成 long 后再参与取模
     */
    private long toLong(final Comparable<?> shardingValue) {
        if (shardingValue instanceof Number number) {
            return number.longValue();
        }
        if (Objects.isNull(shardingValue) || shardingValue.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Sharding value cannot be null or blank.");
        }
        return Long.parseLong(shardingValue.toString().trim());
    }

    private long hashShardingValue(final long id) {
        // 与 Long 的 hashCode 保持一致，保证同一分片键无论以 Long 还是字符串传入，都路由到同一张表
        return Math.abs((long) Long.hashCode(id));
    }
}
