//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.viztool;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import com.samskivert.viztool.PrintUtil;
import com.samskivert.viztool.Visualizer;
import com.samskivert.viztool.clenum.ClassEnumerator;
import com.samskivert.viztool.clenum.FilterEnumerator;
import com.samskivert.viztool.clenum.RegexpEnumerator;
import com.samskivert.viztool.hierarchy.HierarchyVisualizer;
import com.samskivert.viztool.summary.SummaryVisualizer;
import com.samskivert.viztool.util.FontPicker;


@Mojo(name="genviz", defaultPhase=LifecyclePhase.PROCESS_CLASSES,
      requiresDependencyResolution=ResolutionScope.COMPILE)
public class GenVizMojo extends AbstractMojo {

    /** The top-level package shared by all of the to-be-visualized classes. */
    @Parameter(property="pkgroot", required=true)
    public String pkgroot;

    /** A regular expression that matches all classes to be visualized. If none is specified this
     * will be {@code pkgroot\..*}. */
    @Parameter(property="regexp")
    public String regexp;

    /** Selects the visualization mode: {@code hierarchy} or {@code summary}. */
    @Parameter(property="mode", defaultValue="hierarchy")
    public String mode;

    public void execute () throws MojoExecutionException {
        // create the classloader we'll use to load FooRecord classes
        List<URL> entries = new ArrayList<URL>();
        for (String entry : _compileClasspath) addEntry(entries, entry);
        addEntry(entries, _project.getBuild().getOutputDirectory());
        ClassLoader cloader = URLClassLoader.newInstance(
            entries.toArray(new URL[entries.size()]),
            Thread.currentThread().getContextClassLoader());

        // enumerate our classes and print out any warnings
        ClassEnumerator clenum = new ClassEnumerator(_compileClasspath);
        for (String warning : clenum.getWarningStrings()) {
            getLog().warn("Warning: " + warning);
        }

        // initialize the font picker
        FontPicker.init(true);

        // if they supplied no regular expression, make one from their package root
        if (regexp == null) {
            regexp = pkgroot + "\\..*";
        }

        // filter our classes based on the supplied regexp
        FilterEnumerator fenum = null;
        try {
            fenum = new RegexpEnumerator(regexp, null, clenum);
        } catch  (Exception e) {
            throw new MojoExecutionException(
                "Invalid package regular expression [regexp=" + regexp + "].", e);
        }

        List<Class<?>> classes = new ArrayList<Class<?>>();
        while (fenum.hasNext()) {
            String cname = fenum.next();
            // skip inner classes, the visualizations pick those up themselves
            if (cname.indexOf("$") != -1) continue;
            try {
                classes.add(Class.forName(cname, false, cloader));
            } catch (Throwable t) {
                getLog().warn("Unable to introspect class [class=" + cname + "].", t);
            }
        }

        Visualizer viz;
        if ("hierarchy".equalsIgnoreCase(mode)) {
            viz = new HierarchyVisualizer();
        } else if ("summary".equalsIgnoreCase(mode)) {
            viz = new SummaryVisualizer();
        } else {
            throw new MojoExecutionException("Unknown visualization mode: " + mode);
        }
        viz.setPackageRoot(pkgroot);
        viz.setClasses(classes.iterator());

        // create our target directory if needed
        File projectDir = new File(_project.getBuild().getDirectory());
        if (!projectDir.isDirectory()) {
            projectDir.mkdir();
        }
        File output = new File(projectDir, _project.getBuild().getFinalName() + ".pdf");
        // _project.getArtifact().setFile(artifactFile); // TODO: add artifact

        try {
            getLog().info("Generating viz at " + output.getPath());
            if (!PrintUtil.print(viz, output)) {
                getLog().info("Printing cancelled.");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Printing failed.", e);
        }
    }

    protected void addEntry (List<URL> entries, String entry) {
        try {
            entries.add(new File(entry).toURI().toURL());
        } catch (MalformedURLException mue) {
            getLog().warn("Malformed classpath entry: " + entry, mue);
        }
    }

    @Parameter(property="project")
    private MavenProject _project;

    @Parameter(property="project.compileClasspathElements", required=true, readonly=true)
    private List<String> _compileClasspath;
}
