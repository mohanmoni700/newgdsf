package services;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RetryOnFailureAspect
{
    public static final String RETRY_LIMIT_EXCEEDED_MSG = "Retry limit exceeded.";

    private static final int DEFAULT_MAX_RETRIES = 2;

    private int maxRetries = DEFAULT_MAX_RETRIES;

    @Around("execution(* *(..)) && @annotation(retry)")
    public Object retry(ProceedingJoinPoint pjp, RetryOnFailure retry) throws Throwable
    {
        Class exceptionClass = retry.exception();
        int retries = retry.attempts();
        Object returnValue = null;
        int attemptCount = 0;
        do{
            attemptCount++;
            try
            {
                return pjp.proceed();
            }
            catch (Exception ex)
            {
               /*// handleRetryException(pjp, ex, attemptCount, retry);
            	if(!exceptionClass.isInstance(ex)){
            		throw ex;
            	}
            	if(attemptCount > retries){
            		throw ex;
            	}*/
                if(!exceptionClass.isInstance(ex)){
                    throw ex;
                }
                if (attemptCount == 1 + retry.attempts())
                {
                    throw ex;
                }
                else
                {
                    System.out.println(String
                            .format("%s: Attempt %d of %d failed with exception '%s'. Will retry immediately. %s",
                                    pjp.getSignature(), attemptCount,
                                    retry.attempts(),
                                    ex.getClass().getCanonicalName(),
                                    ex.getMessage()));
                }
            }
        }while(attemptCount <= retries);
        return null;
    }

}