package net.kozelka.contentcheck.mojo;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import net.kozelka.contentcheck.introspection.DefaultIntrospector;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Scans content listing of archive specified by {@link #sourceFile}
 * and writes it to file specified into {@link #contentListing}.
 * Only entities matching criteria defined by {@link #checkFilePattern}
 * and {@link #ignoreVendorArchives} are generated.
 */
@Mojo(name = "generate")
public class ContentListingGeneratorMojo extends AbstractArchiveContentMojo {

    /**
     * This parameter allows overwriting existing listing file.
     */
    @Parameter(defaultValue = "false", property = "overwriteExistingListing")
    boolean overwriteExistingListing;

    protected void doExecute() throws IOException, MojoExecutionException, MojoFailureException {
        if(!overwriteExistingListing && contentListing.exists()) {
            throw new MojoFailureException(String.format("Content listing file '%s' already exists. Please set overwriteExistingListing property in plugin configuration or delete this listing file.", contentListing.getPath()));
        }

        final DefaultIntrospector introspector = new DefaultIntrospector(getLog(), ignoreVendorArchives, vendorId, manifestVendorEntry, checkFilesPattern);
        final int count = introspector.readEntries(sourceFile);
        final List<String> sourceEntries = new ArrayList<String>(introspector.getEntries());
        Collections.sort(sourceEntries);
        getLog().info(String.format("The source contains %d entries, but only %d matches the plugin configuration criteria.", count, sourceEntries.size()));
        //TODO: explain/display the criteria

        contentListing.getParentFile().mkdirs();
        final FileWriter writer = new FileWriter(contentListing);
        try {
            writer.write("# Generated by https://code.kozelka.net/contentcheck-maven-plugin\n");
            writer.write("#\n");
            writer.write(String.format("# At '%s' \n", new Date().toString()));
            writer.write(String.format("# Source '%s'\n", sourceFile));
            writer.write(String.format("# Used options: checkFilesPattern='%s' ignoreVendorArchives='%s'\n", checkFilesPattern, Boolean.toString(ignoreVendorArchives)));
            writer.write("#\n");
            writer.write("# Edit this file to configure approved libraries.\n");
            writer.write("\n");
            for (final String entryName : sourceEntries) {
                writer.write(entryName);
                writer.write("\n");
            }
        } finally {
            writer.close();
        }
        getLog().info(String.format("The listing file '%s' has been successfully generated.", contentListing));
    }

}