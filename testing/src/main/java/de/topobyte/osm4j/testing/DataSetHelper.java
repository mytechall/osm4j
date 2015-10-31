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

package de.topobyte.osm4j.testing;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.topobyte.osm4j.core.access.OsmOutputStream;
import de.topobyte.osm4j.core.model.iface.OsmEntity;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmRelation;
import de.topobyte.osm4j.core.model.iface.OsmRelationMember;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;

public class DataSetHelper
{

	public static void write(DataSet data, OsmOutputStream output)
			throws IOException
	{
		for (OsmNode node : data.getNodes()) {
			output.write(node);
		}
		for (OsmWay way : data.getWays()) {
			output.write(way);
		}
		for (OsmRelation relation : data.getRelations()) {
			output.write(relation);
		}
	}

	public static boolean equals(DataSet a, DataSet b)
	{
		if (!nodesEqual(a.getNodes(), b.getNodes())) {
			return false;
		}
		if (!waysEqual(a.getWays(), b.getWays())) {
			return false;
		}
		if (!relationsEqual(a.getRelations(), b.getRelations())) {
			return false;
		}
		return true;
	}

	public static boolean nodesEqual(List<OsmNode> nodes1, List<OsmNode> nodes2)
	{
		if (nodes1.size() != nodes2.size()) {
			return false;
		}
		int n = nodes1.size();
		for (int i = 0; i < n; i++) {
			OsmNode a = nodes1.get(i);
			OsmNode b = nodes2.get(i);
			if (!equals(a, b)) {
				return false;
			}
		}
		return true;
	}

	public static boolean waysEqual(List<OsmWay> ways1, List<OsmWay> ways2)
	{
		if (ways1.size() != ways2.size()) {
			return false;
		}
		int n = ways1.size();
		for (int i = 0; i < n; i++) {
			OsmWay a = ways1.get(i);
			OsmWay b = ways2.get(i);
			if (!equals(a, b)) {
				return false;
			}
		}
		return true;
	}

	public static boolean relationsEqual(List<OsmRelation> relations1,
			List<OsmRelation> relations2)
	{
		if (relations1.size() != relations2.size()) {
			return false;
		}
		int n = relations1.size();
		for (int i = 0; i < n; i++) {
			OsmRelation a = relations1.get(i);
			OsmRelation b = relations2.get(i);
			if (!equals(a, b)) {
				return false;
			}
		}
		return true;
	}

	public static boolean equals(OsmNode a, OsmNode b)
	{
		if (a.getId() != b.getId()) {
			System.out.println("Node ids not equal");
			return false;
		}
		if (Math.abs(a.getLongitude() - b.getLongitude()) > 0.0001) {
			System.out.println("Longitudes not equal");
			return false;
		}
		if (Math.abs(a.getLatitude() - b.getLatitude()) > 0.0001) {
			System.out.println("Latitudes not equal");
			return false;
		}
		if (!tagsEqual(a, b)) {
			System.out.println("Node tags not equal");
			return false;
		}
		return true;
	}

	public static boolean equals(OsmWay a, OsmWay b)
	{
		if (a.getId() != b.getId()) {
			System.out.println("Way ids not equal");
			return false;
		}
		if (!nodeRefsEqual(a, b)) {
			System.out.println("Node references not equal");
			return false;
		}
		if (!tagsEqual(a, b)) {
			System.out.println("Way tags not equal");
			return false;
		}
		return true;
	}

	public static boolean equals(OsmRelation a, OsmRelation b)
	{
		if (a.getId() != b.getId()) {
			System.out.println("Relation ids not equal");
			return false;
		}
		if (!membersEqual(a, b)) {
			System.out.println("Members not equal");
			return false;
		}
		if (!tagsEqual(a, b)) {
			System.out.println("Relation tags not equal");
			return false;
		}
		return true;
	}

	public static boolean tagsEqual(OsmEntity a, OsmEntity b)
	{
		Map<String, String> tagsA = OsmModelUtil.getTagsAsMap(a);
		Map<String, String> tagsB = OsmModelUtil.getTagsAsMap(b);
		if (tagsA.size() != tagsB.size()) {
			return false;
		}
		Set<String> keysA = tagsA.keySet();
		Set<String> keysB = tagsB.keySet();
		if (!keysA.equals(keysB)) {
			return false;
		}
		for (String key : keysA) {
			String valA = tagsA.get(key);
			String valB = tagsB.get(key);
			if (!valA.equals(valB)) {
				return false;
			}
		}
		return true;
	}

	public static boolean nodeRefsEqual(OsmWay a, OsmWay b)
	{
		if (a.getNumberOfNodes() != b.getNumberOfNodes()) {
			System.out.println("Number of node references not equal");
			return false;
		}
		int n = a.getNumberOfNodes();
		for (int i = 0; i < n; i++) {
			if (a.getNodeId(i) != b.getNodeId(i)) {
				return false;
			}
		}
		return true;
	}

	public static boolean membersEqual(OsmRelation a, OsmRelation b)
	{
		if (a.getNumberOfMembers() != b.getNumberOfMembers()) {
			return false;
		}
		int n = a.getNumberOfMembers();
		for (int i = 0; i < n; i++) {
			OsmRelationMember member1 = a.getMember(i);
			OsmRelationMember member2 = b.getMember(i);
			if (!membersEqual(member1, member2)) {
				return false;
			}
		}
		return true;
	}

	public static boolean membersEqual(OsmRelationMember a, OsmRelationMember b)
	{
		if (a.getId() != b.getId()) {
			return false;
		}
		if (a.getType() != b.getType()) {
			return false;
		}
		if (!a.getRole().equals(b.getRole())) {
			return false;
		}
		return true;
	}

}
