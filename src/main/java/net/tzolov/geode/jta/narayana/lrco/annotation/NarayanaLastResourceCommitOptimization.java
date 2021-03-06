/*
 * Copyright 2017. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.tzolov.geode.jta.narayana.lrco.annotation;

import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.lang.annotation.*;

/**
 *
 * The {@link NarayanaLastResourceCommitOptimization} annotation may only be used on a Spring application
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * class that is also annotated with {@link org.springframework.transaction.annotation.EnableTransactionManagement @EnableTransactionManagement}
 * with an explicit {@link EnableTransactionManagement#order} set to value other than {@link Integer#MAX_VALUE} or {@link Integer#MIN_VALUE}.
 *
 * @author Christian Tzolov (christian.tzolov@gmail.com)
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@EnableAspectJAutoProxy
@Import(NarayanaLrcoConfiguration.class)
@SuppressWarnings("unused")
public @interface NarayanaLastResourceCommitOptimization {
}



