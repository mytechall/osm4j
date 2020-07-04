// Copyright 2020 Sebastian Kuerten
//
// This file is part of osm4j.
//
// osm4j is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// osm4j is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with osm4j. If not, see <http://www.gnu.org/licenses/>.

package de.topobyte.osm4j.utils.executables;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.access.OsmIteratorInput;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.utils.AbstractExecutableSingleOutput;
import de.topobyte.osm4j.utils.FileFormat;
import de.topobyte.osm4j.utils.OsmFile;
import de.topobyte.osm4j.utils.OsmFileSetInput;
import de.topobyte.utilities.apache.commons.cli.OptionHelper;

public class OsmMergeMany extends AbstractExecutableSingleOutput
{

	private static final String OPTION_INPUT_FORMAT = "input-format";

	@Override
	protected String getHelpMessage()
	{
		return OsmMergeMany.class.getSimpleName()
				+ " [options] <file> [<file>...]";
	}

	public static void main(String[] args) throws IOException
	{
		OsmMergeMany task = new OsmMergeMany();

		task.setup(args);

		task.init();

		try {
			task.run();
		} catch (IOException e) {
			System.out.println("error while running task");
			e.printStackTrace();
		}

		task.finish();
	}

	public OsmMergeMany()
	{
		// @formatter:off
		OptionHelper.addL(options, OPTION_INPUT_FORMAT, true, true, "the file format of the input files");
		// @formatter:on
	}

	private boolean readMetadata = true;
	private FileFormat inputFormat;
	private List<String> additionalPaths = new ArrayList<>();

	@Override
	protected void setup(String[] args)
	{
		super.setup(args);

		String inputFormatName = line.getOptionValue(OPTION_INPUT_FORMAT);
		inputFormat = FileFormat.parseFileFormat(inputFormatName);
		if (inputFormat == null) {
			System.out.println("invalid input format");
			System.out.println("please specify one of: "
					+ FileFormat.getHumanReadableListOfSupportedFormats());
			System.exit(1);
		}

		String[] additionalArguments = line.getArgs();
		if (additionalArguments.length < 2) {
			System.out.println("please specify at least two files as input");
			System.exit(1);
		}

		for (String arg : additionalArguments) {
			additionalPaths.add(arg);
		}
	}

	private List<OsmFile> osmFiles = new ArrayList<>();

	@Override
	protected void init() throws IOException
	{
		super.init();

		for (String path : additionalPaths) {
			osmFiles.add(new OsmFile(Paths.get(path), inputFormat));
		}
	}

	public void run() throws IOException
	{
		OsmFileSetInput input = new OsmFileSetInput(osmFiles);
		OsmIteratorInput iteratorInput = input.createIterator(true,
				readMetadata);
		OsmIterator iterator = iteratorInput.getIterator();
		while (iterator.hasNext()) {
			EntityContainer entityContainer = iterator.next();
			switch (entityContainer.getType()) {
			case Node:
				osmOutputStream.write((OsmNode) entityContainer.getEntity());
				break;
			case Way:
				osmOutputStream.write((OsmWay) entityContainer.getEntity());
				break;
			case Relation:
				osmOutputStream
						.write((OsmRelation) entityContainer.getEntity());
				break;
			}
		}
		osmOutputStream.complete();
	}

}
