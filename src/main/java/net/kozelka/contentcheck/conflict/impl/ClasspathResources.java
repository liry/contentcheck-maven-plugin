package net.kozelka.contentcheck.conflict.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.kozelka.contentcheck.conflict.api.ConflictCheckResponse;
import net.kozelka.contentcheck.conflict.model.ArchiveInfo;
import net.kozelka.contentcheck.conflict.model.ResourceInfo;

/**
 * Collects classpath and registers all resource conflicts during that
 *
 * @author Petr Kozelka
 */
class ClasspathResources {
    final ConflictCollector conflictCollector = new ConflictCollector();
    final Map<String, ResourceWithOptions> resourcesByUri = new HashMap<String, ResourceWithOptions>();

    public void addResource(ResourceInfo resource, ArchiveInfo archive) {
        final String resourceUri = resource.getUri();
        final String myHash = resource.getHash();
        ResourceWithOptions rwo = resourcesByUri.get(resourceUri);
        if (rwo == null) {
            rwo = new ResourceWithOptions();
            rwo.uri = resourceUri;
            resourcesByUri.put(resourceUri, rwo);
        } else {
            for (ArchiveInfo candidate : rwo.allCandidates) {
                final String hisHash = findResourceByUri(candidate, resourceUri).getHash();
                final boolean isDuplicate =  myHash.equals(hisHash);
                addConflict(archive, candidate, resource, isDuplicate);
                addConflict(candidate, archive, resource, isDuplicate);
            }
        }
        rwo.addCandidate(myHash, archive);
    }

    static ResourceInfo findResourceByUri(ArchiveInfo archive, String resourceUri) {
        for (ResourceInfo resource : archive.getResources()) {
            if (resource.getUri().equals(resourceUri)) {
                return resource;
            }
        }
        return null;
    }

    private void addConflict(ArchiveInfo archive, ArchiveInfo candidate, ResourceInfo resource, boolean isDuplicate) {
        final ConflictCheckResponse.ArchiveConflict ac = conflictCollector.addConflict(candidate, archive, resource);
        if (isDuplicate) {
            ac.addDuplicate(resource);
        } else {
            ac.addConflict(resource);
        }
    }

    public Collection<? extends ConflictCheckResponse.ArchiveConflict> getConflicts() {
        return conflictCollector.getAll();
    }
}
