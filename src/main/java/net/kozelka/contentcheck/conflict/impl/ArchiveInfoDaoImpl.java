package net.kozelka.contentcheck.conflict.impl;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.kozelka.contentcheck.conflict.api.ArchiveInfoDao;
import net.kozelka.contentcheck.conflict.model.ArchiveInfo;
import net.kozelka.contentcheck.conflict.model.ResourceInfo;

/**
 * @author Petr Kozelka
 */
class ArchiveInfoDaoImpl implements ArchiveInfoDao {

    private final Set<ArchiveInfo> exploredArchives = new LinkedHashSet<ArchiveInfo>();
    @Override
    public ArchiveInfo saveArchive(ArchiveInfo archiveInfo) {
        exploredArchives.add(archiveInfo);
        return archiveInfo;
    }

    @Override
    public Set<ArchiveInfo> getAllArchives() {
        return exploredArchives;
    }


    //
    private final Map<String, ResourceInfo> resources = new LinkedHashMap<String, ResourceInfo>();

    @Override
    public ResourceInfo saveResource(ResourceInfo resourceInfo) {
        resources.put(resourceInfo.getUri(), resourceInfo);
        return resourceInfo;
    }

    @Override
    public ResourceInfo findResourceByKey(String key) {
        return resources.get(key);
    }

}