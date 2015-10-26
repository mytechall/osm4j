// Copyright 2015 Sebastian Kuerten
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

package de.topobyte.osm4j.extra.datatree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vividsolutions.jts.geom.Envelope;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.access.OsmOutputStream;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmBounds;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.impl.Bounds;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import de.topobyte.osm4j.pbf.seq.PbfWriter;
import de.topobyte.osm4j.tbo.access.TboIterator;
import de.topobyte.osm4j.tbo.access.TboWriter;
import de.topobyte.osm4j.utils.AbstractTaskSingleInputIterator;
import de.topobyte.osm4j.utils.FileFormat;
import de.topobyte.osm4j.utils.config.PbfConfig;
import de.topobyte.osm4j.utils.config.PbfOptions;
import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;
import de.topobyte.osm4j.xml.output.OsmXmlOutputStream;
import de.topobyte.utilities.apache.commons.cli.OptionHelper;

public class CreateNodeTreeMaxNodes extends AbstractTaskSingleInputIterator
{

	private static final String OPTION_OUTPUT = "output";
	private static final String OPTION_OUTPUT_FORMAT = "output_format";
	private static final String OPTION_MAX_NODES = "max_nodes";

	@Override
	protected String getHelpMessage()
	{
		return CreateNodeTreeMaxNodes.class.getSimpleName() + " [options]";
	}

	public static void main(String[] args) throws IOException
	{
		CreateNodeTreeMaxNodes task = new CreateNodeTreeMaxNodes();

		task.setup(args);

		task.readMetadata = true;

		task.init();

		task.initOutputs();

		task.run();

		task.finish();
	}

	private int maxNodes;
	private String pathOutput;
	private FileFormat outputFormat;
	private PbfConfig pbfConfig;
	private DataTree tree;
	private boolean writeMetadata = true;

	private File dirOutput;
	private Map<Node, Output> outputs = new HashMap<>();

	private class Output
	{

		private File file;
		private OutputStream output;
		private OsmOutputStream osmOutput;
		private int count = 0;

		Output(File file, OutputStream output, OsmOutputStream osmOutput)
		{
			this.file = file;
			this.output = output;
			this.osmOutput = osmOutput;
		}

	}

	public CreateNodeTreeMaxNodes()
	{
		// @formatter:off
		OptionHelper.add(options, OPTION_OUTPUT_FORMAT, true, true, "the file format of the output");
		OptionHelper.add(options, OPTION_OUTPUT, true, true, "directory to store output in");
		OptionHelper.add(options, OPTION_MAX_NODES, true, true, "the maximum number of nodes per file");
		PbfOptions.add(options);
		// @formatter:on
	}

	@Override
	protected void setup(String[] args)
	{
		super.setup(args);

		String outputFormatName = line.getOptionValue(OPTION_OUTPUT_FORMAT);
		outputFormat = FileFormat.parseFileFormat(outputFormatName);
		if (outputFormat == null) {
			System.out.println("invalid output format");
			System.out.println("please specify one of: "
					+ FileFormat.getHumanReadableListOfSupportedFormats());
			System.exit(1);
		}

		pbfConfig = PbfOptions.parse(line);

		pathOutput = line.getOptionValue(OPTION_OUTPUT);
		String argMaxNodes = line.getOptionValue(OPTION_MAX_NODES);

		maxNodes = Integer.parseInt(argMaxNodes);
		if (maxNodes < 1) {
			System.out.println("Please specify a max nodes >= 1");
			System.exit(1);
		}
	}

	@Override
	protected void init() throws FileNotFoundException
	{
		super.init();

		dirOutput = new File(pathOutput);
		if (!dirOutput.exists()) {
			System.out.println("Creating output directory");
			dirOutput.mkdirs();
		}
		if (!dirOutput.isDirectory()) {
			System.out.println("Output path is not a directory");
			System.exit(1);
		}
		if (dirOutput.list().length != 0) {
			System.out.println("Output directory is not empty");
			System.exit(1);
		}

		if (!inputIterator.hasBounds()) {
			System.out.println("Input does not provide bounds");
			System.exit(1);
		}
	}

	protected void initOutputs() throws IOException
	{
		OsmBounds bounds = inputIterator.getBounds();
		Envelope envelope = new Envelope(bounds.getLeft(), bounds.getRight(),
				bounds.getBottom(), bounds.getTop());

		tree = new DataTree(envelope);

		System.out.println("bounds: " + bounds);
		tree.print();

		List<Node> leafs = tree.getLeafs();
		for (Node leaf : leafs) {
			init(leaf);
		}
	}

	private Output init(Node leaf) throws IOException
	{
		String filename = Integer.toHexString(leaf.getPath()) + ".pbf";
		File file = new File(dirOutput, filename);
		System.out.println(file + ": " + leaf.getEnvelope());
		OutputStream os = new FileOutputStream(file);
		OsmOutputStream osmOutput = setupOsmOutput(os);
		Output output = new Output(file, os, osmOutput);
		outputs.put(leaf, output);

		Envelope box = leaf.getEnvelope();
		osmOutput.write(new Bounds(box.getMinX(), box.getMaxX(), box.getMaxY(),
				box.getMinY()));

		return output;
	}

	private OsmIterator setupOsmInput(InputStream in)
	{
		switch (outputFormat) {
		default:
		case TBO:
			return new TboIterator(in);
		case XML:
			return new OsmXmlIterator(in, writeMetadata);
		case PBF:
			return new PbfIterator(in, writeMetadata);
		}
	}

	private OsmOutputStream setupOsmOutput(OutputStream out)
	{
		switch (outputFormat) {
		default:
		case TBO:
			return new TboWriter(out);
		case XML:
			return new OsmXmlOutputStream(out, writeMetadata);
		case PBF:
			PbfWriter pbfWriter = new PbfWriter(out, writeMetadata);
			pbfWriter.setCompression(pbfConfig.getCompression());
			pbfWriter.setUseDense(pbfConfig.isUseDenseNodes());
			return pbfWriter;
		}
	}

	protected void run() throws IOException
	{
		loop: while (inputIterator.hasNext()) {
			EntityContainer entityContainer = inputIterator.next();
			switch (entityContainer.getType()) {
			case Node:
				handle((OsmNode) entityContainer.getEntity());
				break;
			case Way:
				break loop;
			case Relation:
				break loop;
			}
		}
	}

	private void handle(OsmNode node) throws IOException
	{
		List<Node> leafs = tree.query(node.getLongitude(), node.getLatitude());
		for (Node leaf : leafs) {
			if (leaf.getEnvelope().contains(node.getLongitude(),
					node.getLatitude())) {
				Output output = outputs.get(leaf);
				output.count++;
				output.osmOutput.write(node);
				if (output.count > maxNodes) {
					split(leaf);
				}
			}
		}
	}

	private void split(Node leaf) throws IOException
	{
		leaf.split();
		Node left = leaf.getLeft();
		Node right = leaf.getRight();

		System.out.println("Splitting: " + leaf.getEnvelope());
		System.out.println(String.format("%s -> %s %s",
				Integer.toHexString(leaf.getPath()),
				Integer.toHexString(left.getPath()),
				Integer.toHexString(right.getPath())));

		Output output = outputs.get(leaf);
		output.osmOutput.complete();
		output.output.close();
		outputs.remove(leaf);

		Output outLeft = init(left);
		Output outRight = init(right);

		FileInputStream input = new FileInputStream(output.file);
		OsmIterator iterator = setupOsmInput(input);
		while (iterator.hasNext()) {
			EntityContainer container = iterator.next();
			if (container.getType() != EntityType.Node) {
				continue;
			}
			OsmNode node = (OsmNode) container.getEntity();
			double lon = node.getLongitude();
			double lat = node.getLatitude();
			if (left.getEnvelope().contains(lon, lat)) {
				outLeft.count++;
				outLeft.osmOutput.write(node);
			}
			if (right.getEnvelope().contains(lon, lat)) {
				outRight.count++;
				outRight.osmOutput.write(node);
			}
		}

		System.out.println("Deleting " + Integer.toHexString(leaf.getPath()));
		output.file.delete();
	}

	@Override
	protected void finish() throws IOException
	{
		super.finish();

		for (Output output : outputs.values()) {
			output.osmOutput.complete();
			output.output.close();
		}
	}

}
