/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2017 Jean-Rémy Falleri <jr.falleri@gmail.com>
 */

package com.github.gumtreediff.client.diff;

import com.github.gumtreediff.io.ActionsIoUtils;
import com.github.gumtreediff.io.DirectoryComparator;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.actions.Diff;
import com.github.gumtreediff.utils.Pair;
import com.github.gumtreediff.client.Option;
import com.github.gumtreediff.client.diff.webdiff.VanillaDiffView;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

@Register(name = "htmldiff", description = "Dump diff as HTML in stdout",
        options = HtmlDiff.HtmlDiffOptions.class)
public class HtmlDiff extends AbstractDiffClient<HtmlDiff.HtmlDiffOptions> {

    public HtmlDiff(String[] args) {
        super(args);
    }

    public static class HtmlDiffOptions extends AbstractDiffClient.DiffOptions {
        protected String output;

        @Override
        public Option[] values() {
            return Option.Context.addValue(super.values(),
                    new Option("-o", "output file", 1) {
                        @Override
                        protected void process(String name, String[] args) {
                            output = args[0];
                        }
                    }
            );
        }
    }

    @Override
    protected HtmlDiffOptions newOptions() {
        return new HtmlDiffOptions();
    }

    @Override
    public void run() throws Exception {
        String version = "/new-gumtree/";
        DirectoryComparator comparator = new DirectoryComparator(opts.srcPath + "/before", opts.srcPath + "/after");
        comparator.compare();

        int i = 0;
        var files = comparator.getModifiedFiles();
        files.sort(Comparator.comparing(pair -> pair.first.getName(), String.CASE_INSENSITIVE_ORDER));
        for (Pair<File, File> pair : files) {
            var diff = getDiff(pair.first.getAbsolutePath(), pair.second.getAbsolutePath());
            var html = VanillaDiffView.build(pair.first, pair.second, diff, true);
            Path htmlFilePath = Paths.get(opts.dstPath + "/web-diff" + version + String.format("%04d", i) + ".html");
            File htmlOutput = new File(htmlFilePath.toString());
            FileWriter writer = new FileWriter(htmlOutput);
            writer.write(html.render());
            writer.close();

            var format = TextDiff.OutputFormat.JSON;
            ActionsIoUtils.ActionSerializer serializer = format.getSerializer(diff.src, diff.editScript, diff.mappings);
            Path actionFilePath = Paths.get(opts.dstPath + "/action-diff" + version + String.format("%04d", i) + ".json");
            File actionOutput = new File(actionFilePath.toString());
            serializer.writeTo(actionOutput);

            System.out.println(pair.first);

            i++;
        }
    }
}
