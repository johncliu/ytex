package ytex.uima;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 
 * @author vijay
 */
public class ApplicationContextHolder {
	private static final Log log = LogFactory
			.getLog(ApplicationContextHolder.class);
	private static Properties ytexProperties;
	private static BeanFactoryLocator beanFactory;
	private static ApplicationContext ytexApplicationContext;
	private static ApplicationContext simApplicationContext;

	static {
		InputStream ytexPropsIn = null;
		String beanRefContext = "classpath*:beanRefContext.xml";
		try {
			ytexPropsIn = ApplicationContextHolder.class
					.getResourceAsStream("/ytex.properties");
			ytexProperties = new Properties();
			ytexProperties.load(ytexPropsIn);
			ytexProperties.putAll(System.getProperties());
			beanRefContext = ytexProperties.getProperty("ytex.beanRefContext",
					beanRefContext);
		} catch (Exception e) {
			log.error("initalizer", e);
		} finally {
			if (ytexPropsIn != null) {
				try {
					ytexPropsIn.close();
				} catch (IOException e) {
				}
			}
		}
		if (log.isInfoEnabled())
			log.info("beanRefContext=" + beanRefContext);
		beanFactory = ContextSingletonBeanFactoryLocator
				.getInstance(beanRefContext);
		ytexApplicationContext = (ApplicationContext) beanFactory
				.useBeanFactory("ytexApplicationContext").getFactory();
	}

	/**
	 * only load the sim application context if it is requested - the concept
	 * graph takes a lot of memory
	 * 
	 * @return
	 */
	public static synchronized ApplicationContext getSimApplicationContext() {
		if (simApplicationContext == null) {
			simApplicationContext = new ClassPathXmlApplicationContext(
					new String[] { "ytex/beans-datasource.xml",
							"ytex/beans-kernel.hbm.xml",
							"ytex/beans-kernel.xml",
							"ytex/beans-kernel-sim.xml" },
					getApplicationContext());
		}
		return simApplicationContext;
	}

	public static ApplicationContext getApplicationContext() {
		return ytexApplicationContext;
	}

	public static Properties getYtexProperties() {
		return ytexProperties;
	}

}
