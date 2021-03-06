package com.example.demo.config;

import com.example.demo.common.UserUtils;
import com.example.demo.entity.RespBean;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
	@Autowired
	UserService userService;
	@Autowired
	CustomAccess customAccess;
	@Autowired
	AuthenticationAccessDeniedHandler deniedHandler;
	@Autowired
	CustomFilter customFilter;

	@Override
	protected void configure(AuthenticationManagerBuilder auth)
			throws Exception {
		auth.userDetailsService(userService)
				.passwordEncoder(new BCryptPasswordEncoder());
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers("/index.html", "/static/***", "/login_page", "/reg/**");
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
				.withObjectPostProcessor(new ObjectPostProcessor<FilterSecurityInterceptor>() {
					@Override
					public <O extends FilterSecurityInterceptor> O postProcess(O o) {
						o.setSecurityMetadataSource(customFilter);
						o.setAccessDecisionManager(customAccess);
						return o;
					}
				})
				.and()
				.formLogin().loginPage("/login_page").loginProcessingUrl("/login")
				.usernameParameter("username").passwordParameter("password")
				.failureHandler(new AuthenticationFailureHandler() {
					@Override
					public void onAuthenticationFailure(HttpServletRequest req,
														HttpServletResponse resp,
														AuthenticationException e)
							throws IOException {
						resp.setContentType("application/json;charset=UTF-8");
						RespBean respBean = null;
						if (e instanceof BadCredentialsException ||
								e instanceof UsernameNotFoundException) {
							respBean = RespBean.error("账号或者密码输入错误");
						} else if (e instanceof LockedException) {
							respBean = RespBean.error("账号被锁定，请联系管理员");
						} else if (e instanceof CredentialsExpiredException) {
							respBean = RespBean.error("密码过期，请联系管理员");
						} else if (e instanceof AccountExpiredException) {
							respBean = RespBean.error("账号被过期，请联系管理员");
						} else if (e instanceof DisabledException) {
							respBean = RespBean.error("账号被禁用，请联系管理员");
						} else {
							respBean = RespBean.error("登陆失败！");
						}
						resp.setStatus(401);
						ObjectMapper om = new ObjectMapper();
						PrintWriter out = resp.getWriter();
						out.write(om.writeValueAsString(respBean));
						out.flush();
						out.close();
					}
				})
				.successHandler(new AuthenticationSuccessHandler() {
					@Override
					public void onAuthenticationSuccess(HttpServletRequest req,
														HttpServletResponse resp,
														Authentication auth)
							throws IOException {
						resp.setContentType("application/json;charset=UTF-8");
						RespBean respBean = RespBean.ok("登陆成功", UserUtils.getCurrentUser());
						ObjectMapper om = new ObjectMapper();
						PrintWriter out = resp.getWriter();
						out.write(om.writeValueAsString(respBean));
						out.flush();
						out.close();

					}
				})
				.permitAll()
				.and()
				.logout()
				.logoutUrl("/logout")
				.logoutSuccessHandler(new LogoutSuccessHandler() {
					@Override
					public void onLogoutSuccess(HttpServletRequest req,
												HttpServletResponse resp,
												Authentication auth)
							throws IOException, ServletException {
						resp.setContentType("application/json;charset=UTF-8");
						RespBean respBean = RespBean.ok("注销成功");
						ObjectMapper om = new ObjectMapper();
						PrintWriter out = resp.getWriter();
						out.write(om.writeValueAsString(respBean));
						out.flush();
						out.close();
					}
				})
				.permitAll()
				.and().csrf().disable()
				.exceptionHandling().accessDeniedHandler(deniedHandler);
	}
}
