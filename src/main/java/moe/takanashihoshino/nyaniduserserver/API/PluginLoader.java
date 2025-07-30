package moe.takanashihoshino.nyaniduserserver.API;


import lombok.SneakyThrows;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

@Component
public class PluginLoader implements  ApplicationListener<ContextClosedEvent>, InitializingBean {

    private static final String PLUGINS_DIR = "plugins";
    private static final Logger logger = Logger.getLogger("NyanID");


    @Override
    public void afterPropertiesSet() throws Exception {
        List<URL> urls = getUrls();
        if (urls != null && !urls.isEmpty()) {
            for (URL url : urls) {
                Plugin plugin = getPlugin(Collections.singletonList(url));
                if (plugin != null ) {
                    logger.info("[NyanID-UserServer] [" + LocalDateTime.now() + "] : Loading :" + plugin.PluginName());
                    plugin.onLoad();

                } else {
                    logger.warning("Failed to load plugin." );
                }
            }
        }else{
            logger.warning("No plugins found.");
        }
    }
    public void run(String... args) throws Exception {

    }
    private List<URL> getUrls() throws IOException {
        File pluginsDir = new File(PLUGINS_DIR);
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            logger.warning("Plugins directory does not exist or is not a directory.");
            System.exit(1);
            return null;
        } else {
            List<URL> jarUrls = new ArrayList<>();
            File[] jarFiles = pluginsDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles != null) {
                for (File jarFile : jarFiles) {
                    jarUrls.add(jarFile.toURI().toURL());
                }
            }
            return jarUrls;
        }
    }

    private Plugin getPlugin(List<URL> jarUrls) throws NoSuchMethodException, ClassNotFoundException, IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        URLClassLoader classLoader = new URLClassLoader(jarUrls.toArray(new URL[0]), getClass().getClassLoader());
        for (URL url : jarUrls) {
            try (JarFile jarFile = new JarFile(url.getFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        String className = entry.getName().substring(0, entry.getName().length() - 6).replace('/', '.');
                        Class<?> clazz = classLoader.loadClass(className);
                        if (Plugin.class.isAssignableFrom(clazz)) {
                            return (Plugin) clazz.getDeclaredConstructor().newInstance();
                        }
                    }
                }
            }
        }
        return null;
    }

    @SneakyThrows
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        Objects.requireNonNull(getPlugin(getUrls())).onDisable();
    }


}