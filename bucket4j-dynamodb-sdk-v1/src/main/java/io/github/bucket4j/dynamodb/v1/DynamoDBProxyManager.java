package io.github.bucket4j.dynamodb.v1;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;

import java.nio.charset.StandardCharsets;

/**
 * The extension of Bucket4j library addressed to support <a href="https://aws.amazon.com/dynamodb/">AWS DynamoDB</a>
 * as backend via <a href="https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html">AWS SDK for
 * Java 1.x</a>.
 * <p>
 * Example of CloudFormation definition of table:
 * <pre>
 * {@code
 * Resources:
 *   RateLimitsTable:
 *     Type: 'AWS::DynamoDB::Table'
 *     Properties:
 *       # Table name can be specified in the next property
 *       # (uncomment it) or be auto-generated (leave as is).
 *       # TableName: rate-limits
 *       AttributeDefinitions:
 *           # Attribute name can not be altered at the moment
 *           # and should be 'key'.
 *         - AttributeName: 'key'
 *           # Only S and N types are supported at the moment.
 *           # Use S if you want to use string keys and N for
 *           # long keys.
 *           AttributeType: 'S'
 *           # Actual state will be stored in 'state' attribute
 *           # (name can not be altered at the moment) but it is
 *           # not a part of primary key so is not defined.
 *       KeySchema:
 *         - AttributeName: 'key'
 *           KeyType: 'HASH'
 *       # On-demand billing more is used to simplify example,
 *       # provisioned capacity can be used as well.
 *       BillingMode: 'PAY_PER_REQUEST'
 * }
 * </pre>
 * Table that is on-demand and can be used with {@link #stringKey(AmazonDynamoDB, String, ClientSideConfig)}.
 */
public final class DynamoDBProxyManager {
    /**
     * Returns implementation that uses {@link String} (DynamoDB String) for keys.
     *
     * <b>NOTE</b>: DynamoDB supports at most 2048 bytes (in {@link StandardCharsets#UTF_8}) for
     * primary {@link String} keys.
     *
     * @param db     {@link AmazonDynamoDB} client to access DynamoDB table.
     * @param table  name of DynamoDB table.
     * @param config {@link ClientSideConfig} configuration.
     */
    public static BaseDynamoDBProxyManager<String> stringKey(AmazonDynamoDB db, String table, ClientSideConfig config) {
        return new StringDynamoDBProxyManager(db, table, config);
    }

    /**
     * Returns implementation that uses {@link Long} (DynamoDB Number) for keys.
     *
     * @param db     {@link AmazonDynamoDB} client to access DynamoDB table.
     * @param table  name of DynamoDB table.
     * @param config {@link ClientSideConfig} configuration.
     */
    public static BaseDynamoDBProxyManager<Long> longKey(AmazonDynamoDB db, String table, ClientSideConfig config) {
        return new LongDynamoDBProxyManager(db, table, config);
    }

    private DynamoDBProxyManager() {}
}
