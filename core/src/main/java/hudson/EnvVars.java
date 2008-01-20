package hudson;

import hudson.remoting.VirtualChannel;
import hudson.remoting.Callable;

import java.io.File;
import java.io.Serializable;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Comparator;
import java.util.Collections;

/**
 * Environment variables.
 *
 * <p>
 * In Hudson, often we need to build up "environment variable overrides"
 * on master, then to execute the process on slaves. This causes a problem
 * when working with variables like <tt>PATH</tt>. So to make this work,
 * we introduce a special convention <tt>PATH+FOO</tt> &mdash; all entries
 * that starts with <tt>PATH+</tt> are merged and prepended to the inherited
 * <tt>PATH</tt> variable, on the process where a new process is executed. 
 * 
 * @author Kohsuke Kawaguchi
 */
public class EnvVars extends TreeMap<String,String> {

    public EnvVars() {
        super(CASE_INSENSITIVE_COMPARATOR);
    }

    public EnvVars(Map<String,String> m) {
        super(CASE_INSENSITIVE_COMPARATOR);
        putAll(m);
    }

    /**
     * Overrides the current entry by the given entry.
     *
     * <p>
     * Handles <tt>PATH+XYZ</tt> notation.
     */
    public void override(String key, String value) {
        if(value==null || value.length()==0) {
            remove(key);
            return;
        }

        int idx = key.indexOf('+');
        if(idx>0) {
            String realKey = key.substring(0,idx);
            String v = get(realKey);
            if(v==null) v=value;
            else        v=value+File.pathSeparatorChar+v;
            put(realKey,v);
            return;
        }

        put(key,value);
    }

    public void overrideAll(Map<String,String> all) {
        for (Map.Entry<String, String> e : all.entrySet()) {
            override(e.getKey(),e.getValue());
        }
    }

    /**
     * Obtains the environment variables of a remote peer.
     *
     * @param channel
     *      Can be null, in which case the map indicating "N/A" will be returned.
     */
    public static Map<String,String> getRemote(VirtualChannel channel) throws IOException, InterruptedException {
        if(channel==null)
            return Collections.singletonMap("N/A","N/A");
        return new EnvVars(channel.call(new GetEnvVars()));
    }

    private static final class GetEnvVars implements Callable<Map<String,String>,RuntimeException> {
        public Map<String,String> call() {
            return new TreeMap<String,String>(EnvVars.masterEnvVars);
        }
        private static final long serialVersionUID = 1L;
    }


    /**
     * Compares strings case insensitively.
     */
    private static final Comparator<String> CASE_INSENSITIVE_COMPARATOR = new CaseInsensitiveComparator();
    
    /**
     * Environmental variables that we've inherited.
     */
    public static final Map<String,String> masterEnvVars = new EnvVars(System.getenv());

    private static class CaseInsensitiveComparator implements Comparator<String>, Serializable {
        public int compare(String lhs, String rhs) {
            return lhs.compareToIgnoreCase(rhs);
        }
        private static final long serialVersionUID = 1L;
    }
}
