package com.ruoyi.web.controller.business;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Collections;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
import com.ruoyi.RuoYiApplication;
import com.ruoyi.web.core.config.BusinessSchedulingConfig;

public class BusinessSchedulingConfigTest
{
    private boolean enabled(String value)
    {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
        {
            if (value != null)
                context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("exercise",
                        Collections.singletonMap("app.scheduling.enabled", value)));
            context.register(BusinessSchedulingConfig.class);
            context.refresh();
            return context.containsBean(TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME);
        }
    }

    @Test public void defaultsToEnabledForExistingDeployments() { assertTrue(enabled(null)); }
    @Test public void explicitlyEnabledRegistersScheduledProcessing() { assertTrue(enabled("true")); }
    @Test public void recoveryCanDisableAllAnnotatedBackgroundJobs()
    {
        assertFalse(enabled("false"));
        assertFalse(RuoYiApplication.class.isAnnotationPresent(EnableScheduling.class));
    }
}
