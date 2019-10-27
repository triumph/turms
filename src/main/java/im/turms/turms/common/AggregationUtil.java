/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.turms.common;

import im.turms.turms.pojo.domain.Message;
import org.bson.Document;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

import static im.turms.turms.common.Constants.TOTAL;

public class AggregationUtil {
    private AggregationUtil() {
    }

    public static Mono<Long> countDistinct(
            @NotNull ReactiveMongoTemplate mongoTemplate,
            @NotNull Criteria criteria,
            @NotNull String field,
            Class<?> clazz) {
        return mongoTemplate.aggregate(Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.project(field),
                Aggregation.group(field),
                Aggregation.count().as(TOTAL))
                , clazz, Document.class)
                .single()
                .map(document -> Long.valueOf((Integer) document.get(TOTAL)))
                .defaultIfEmpty(0L);
    }
}
