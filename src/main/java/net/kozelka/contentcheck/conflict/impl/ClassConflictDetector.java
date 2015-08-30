package net.kozelka.contentcheck.conflict.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.kozelka.contentcheck.conflict.api.ArchiveInfoDao;
import net.kozelka.contentcheck.conflict.api.ConflictCheckResponse;
import net.kozelka.contentcheck.conflict.model.ArchiveInfo;
import net.kozelka.contentcheck.conflict.model.ConflictingArchive;
import net.kozelka.contentcheck.conflict.model.ResourceInfo;
import net.kozelka.contentcheck.introspection.ContentIntrospector;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * @todo improve design
 * @todo write tests
 * @author Petr Kozelka
 */
public class ClassConflictDetector {
    private final ArchiveInfoDao archiveInfoDao = new ArchiveInfoDaoImpl();

    private ArchiveInfo exploreArchive(ZipInputStream zis, String archiveName) throws IOException {
//        System.out.println("exploreArchive: " + archiveName);
        final ArchiveInfo archiveInfo = new ArchiveInfo();
        archiveInfo.setKey(archiveName);
        ZipEntry entry = zis.getNextEntry();
        int classCount = 0;
        while (entry != null) {
            final String entryName = entry.getName();
            if (entryName.endsWith(".class")) {
                processClassResource(archiveInfo, entry);
                classCount ++;
            }
            //
            zis.closeEntry();
            entry = zis.getNextEntry();
        }
        archiveInfo.setClassCount(classCount);
        return archiveInfoDao.saveArchive(archiveInfo);
    }

    private void processClassResource(ArchiveInfo archiveInfo, ZipEntry entry) {
        ResourceInfo resourceInfo = new ResourceInfo();
        resourceInfo.setUri(entry.getName());
        resourceInfo.setHash("crc:" + entry.getCrc());
        resourceInfo = archiveInfoDao.saveResource(resourceInfo);

        //TODO following prepares the results and should be probably moved elsewhere
        for (ArchiveInfo hostingArchive : resourceInfo.getHostingArchives()) {
            addConflict(hostingArchive, archiveInfo, resourceInfo);
            addConflict(archiveInfo, hostingArchive, resourceInfo);
        }
        //
        resourceInfo.getHostingArchives().add(archiveInfo);
    }

    private void addConflict(ArchiveInfo thisArchive, ArchiveInfo thatArchive, ResourceInfo conflictingResource) {
        final String conflictingArchiveKey = thatArchive.getKey();
        ConflictingArchive conflictingArchive = findConflictingArchiveByKey(thisArchive, conflictingArchiveKey);
        if (conflictingArchive == null) {
            conflictingArchive = new ConflictingArchive();
            conflictingArchive.setConflictingArchive(thatArchive);
            thisArchive.getConflictingArchives().add(conflictingArchive);
        }
        conflictingArchive.addResource(conflictingResource);
    }

    private static ConflictingArchive findConflictingArchiveByKey(ArchiveInfo thisArchive, String key) {
        for (ConflictingArchive conflictingArchive : thisArchive.getConflictingArchives()) {
            if (key.equals(conflictingArchive.getArchiveInfo().getKey())) {
                return conflictingArchive;
            }
        }
        return null;
    }

    public ConflictCheckResponse exploreWar(File war) throws IOException {
        final ContentIntrospector ci = new ContentIntrospector();
        ci.setSourceFile(war);
        final ConflictCheckResponse response = new ConflictCheckResponse();
        ci.setEntryContentFilter(new ContentIntrospector.EntryContentFilter() {
            @Override
            public boolean accept(String entryName, InputStream entryContentStream) throws IOException {
                if (entryName.startsWith("WEB-INF/lib/") && entryName.endsWith(".jar")) {
                    final ZipInputStream zis = new ZipInputStream(entryContentStream);
                    ClassConflictDetector.this.exploreArchive(zis, entryName);
                    response.incrementExploredArchiveCount();
                }
                //TODO: add support for WEB-INF/classes as another resource
                return false;
            }
        });
        ci.walk();

        // fill response
        final List<ArchiveInfo> conflictingArchives = response.getConflictingArchives();
        for (ArchiveInfo archive : archiveInfoDao.getAllArchives()) {
            if (! archive.getConflictingArchives().isEmpty()) {
                conflictingArchives.add(archive);
            }
        }
        return response;
    }

    //TODO: move this to cli
    public static void main(String[] args) throws IOException {
        final int previewThreshold = 5;
        final File war = new File(args[0]);

        System.out.println("Detecting conflict in " + war);
        System.out.println("Class preview threshold: " + previewThreshold);
        final ClassConflictDetector ccd = new ClassConflictDetector();
        final ConflictCheckResponse response = ccd.exploreWar(war);
        ConflictCheckResponsePrinter.printResults(response, previewThreshold, new StreamConsumer() {
            @Override
            public void consumeLine(String line) {
                System.out.println(line);
            }
        });
    }
}
