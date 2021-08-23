package com.drkiettran.saml2_example.aspect;

import java.time.Duration;
import java.time.Instant;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Aspect
public class LoggingAspect {
	private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

	// AOP expression for which methods shall be intercepted
	@Around("execution(* com.drkiettran..*(..)))")
	public Object profileAllMethods(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

		MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();

		String className = methodSignature.getDeclaringType().getSimpleName();
		String methodName = methodSignature.getName();
		logger.info("==> " + className + "." + methodName + " starts ...");

		Instant start = Instant.now();
		Object result = proceedingJoinPoint.proceed();
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();

		logger.info("==> Execution time of " + className + "." + methodName + " :: " + timeElapsed + " ms");
		logger.info("==> " + className + "." + methodName + " ends ...");
		return result;
	}
}
