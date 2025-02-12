/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * Declarative configuration for a load balancer client. Add this annotation to any
 * <code>@Configuration</code> and then inject a {@link LoadBalancerClientFactory} to
 * access the client that is created.
 * 用于一个load balancer客户端的声明式配置。将该注解添加到任何{@link Configuration}上，
 * 然后将注入{@link LoadBalancerClientFactory}到被创建的客户端中
 *
 * @author Dave Syer
 * @see LoadBalancerClient
 * @see LoadBalancerClientConfigurationRegistrar
 */
@Configuration(proxyBeanMethods = false)
@Import(LoadBalancerClientConfigurationRegistrar.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoadBalancerClient {

	/**
	 * @return load balancer 客户端的名称
	 * @see #name()
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * @return load balancer 客户端的名称，用于定义唯一的客户端资源
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * 用于load balancer客户端的自定义的{@link Configuration}。
	 * 可以包含组成客户端的各个部分的覆盖{@link org.springframework.context.annotation.Bean}定义。
	 *
	 * @return 用于load balancer客户端的配置类
	 * @see LoadBalancerClientConfiguration for the defaults
	 */
	Class<?>[] configuration() default {};

}
