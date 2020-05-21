/*
 * Copyright (c) 2020 Dzikoysk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.panda_lang.reposilite.metadata;

import io.vavr.collection.Stream;
import org.panda_lang.reposilite.utils.FilesUtils;
import org.panda_lang.utilities.commons.StringUtils;
import org.panda_lang.utilities.commons.text.ContentJoiner;

import java.io.File;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class MetadataUtils {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                    .withLocale(Locale.getDefault())
                    .withZone(ZoneOffset.UTC);

    private static final Comparator<String> REVERSED_STRING_COMPARATOR = Comparator.reverseOrder();
    private static final Comparator<File> REVERSED_FILE_COMPARATOR = (file, to) -> REVERSED_STRING_COMPARATOR.compare(file.getName(), to.getName());

    private MetadataUtils() { }

    public static File[] toSortedBuilds(File artifactDirectory) {
        return Stream.of(FilesUtils.listFiles(artifactDirectory))
                .filter(File::isFile)
                .filter(file -> file.getName().endsWith(".jar"))
                .sorted(REVERSED_FILE_COMPARATOR) // reversed order
                .toJavaArray(File[]::new);
    }

    protected static File[] toSortedVersions(File artifactDirectory) {
        return Stream.of(FilesUtils.listFiles(artifactDirectory))
                .filter(File::isDirectory)
                .sorted(REVERSED_FILE_COMPARATOR) // reversed order
                .toJavaArray(File[]::new);
    }

    protected static String[] toSortedIdentifiers(String artifact, String version, File[] builds) {
        return Stream.of(builds)
                .map(build -> toIdentifier(artifact, version, build))
                .filterNot(StringUtils::isEmpty)
                .distinct()
                .sorted(REVERSED_STRING_COMPARATOR)
                .toJavaArray(String[]::new);
    }

    protected static File[] toBuildFiles(File artifactDirectory, String identifier) {
        return Stream.of(FilesUtils.listFiles(artifactDirectory))
                .filter(file -> file.getName().contains(identifier + ".") || file.getName().contains(identifier + "-"))
                .filterNot(file -> file.getName().endsWith(".md5"))
                .filterNot(file -> file.getName().endsWith(".sha1"))
                .sorted(REVERSED_FILE_COMPARATOR)
                .toJavaArray(File[]::new);
    }

    public static String toIdentifier(String artifact, String version, File build) {
        String identifier = build.getName();
        identifier = StringUtils.replace(identifier, "." + FilesUtils.getExtension(identifier), StringUtils.EMPTY);
        identifier = StringUtils.replace(identifier, artifact + "-", StringUtils.EMPTY);
        identifier = StringUtils.replace(identifier, version + "-", StringUtils.EMPTY);
        return declassifyIdentifier(identifier);
    }

    public static List<String> toNames(File[] files) {
        return Stream.of(files)
                .map(File::getName)
                .toJavaList();
    }

    private static String declassifyIdentifier(String identifier) {
        int occurrences = StringUtils.countOccurrences(identifier, "-");

        // no action required
        if (occurrences == 0) {
            return identifier;
        }

        int occurrence = identifier.indexOf("-");

        // process identifiers without classifier or build number
        if (occurrences == 1) {
            return isBuildNumber(identifier.substring(occurrence + 1)) ? identifier : identifier.substring(0, occurrence);
        }

        // remove classifier
        return isBuildNumber(identifier.substring(occurrence + 1, identifier.indexOf("-", occurrence + 1)))
                ? identifier.substring(0, identifier.indexOf("-", occurrence + 1))
                : identifier.substring(0, occurrence);
    }

    protected static String toUpdateTime(File file) {
        return TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(file.lastModified()));
    }

    public static String toGroup(String[] elements) {
        return ContentJoiner.on(".")
                .join(Arrays.copyOfRange(elements, 0, elements.length))
                .toString();
    }

    protected static String toGroup(String[] elements, int toShrink) {
        return toGroup(shrinkGroup(elements, toShrink));
    }

    protected static String[] shrinkGroup(String[] elements, int toShrink) {
        return Arrays.copyOfRange(elements, 0, elements.length - toShrink);
    }

    private static boolean isBuildNumber(String content) {
        for (char character : content.toCharArray()) {
            if (!Character.isDigit(character)) {
                return false;
            }
        }

        return true;
    }
    public static <T> T getLatest(T[] elements) {
        return elements.length > 0 ? elements[0] : null;
    }

    public static <T> T getLast(T[] elements) {
        return  elements.length == 0 ? null : elements[elements.length - 1];
    }

}
