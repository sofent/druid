/*
 * Druid - a distributed column store.
 * Copyright (C) 2012  Metamarkets Group Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.metamx.druid.indexer.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.metamx.common.exception.FormattedException;
import com.metamx.common.parsers.Parser;
import com.metamx.common.parsers.ToLowerCaseParser;
import com.metamx.druid.input.InputRow;
import com.metamx.druid.input.MapBasedInputRow;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 */
public class StringInputRowParser
{
  private final TimestampSpec timestampSpec;
  private final DataSpec dataSpec;

  private final Set<String> dimensionExclusions;
  private final Parser<String, Object> parser;

  @JsonCreator
  public StringInputRowParser(
      @JsonProperty("timestampSpec") TimestampSpec timestampSpec,
      @JsonProperty("data") DataSpec dataSpec,
      @JsonProperty("dimensionExclusions") List<String> dimensionExclusions
  )
  {
    this.timestampSpec = timestampSpec;
    this.dataSpec = dataSpec;

    this.dimensionExclusions = Sets.newHashSet();
    if (dimensionExclusions != null) {
      for (String dimensionExclusion : dimensionExclusions) {
        this.dimensionExclusions.add(dimensionExclusion.toLowerCase());
      }
    }
    this.dimensionExclusions.add(timestampSpec.getTimestampColumn());

    this.parser = new ToLowerCaseParser(dataSpec.getParser());
  }

  public StringInputRowParser addDimensionExclusion(String dimension)
  {
    dimensionExclusions.add(dimension);
    return this;
  }

  public InputRow parse(String input) throws FormattedException
  {
    Map<String, Object> theMap = parser.parse(input);

    final List<String> dimensions = dataSpec.hasCustomDimensions()
                                    ? dataSpec.getDimensions()
                                    : Lists.newArrayList(Sets.difference(theMap.keySet(), dimensionExclusions));

    final DateTime timestamp = timestampSpec.extractTimestamp(theMap);
    if (timestamp == null) {
      throw new NullPointerException(
          String.format(
              "Null timestamp in input string: %s",
              input.length() < 100 ? input : input.substring(0, 100) + "..."
          )
      );
    }

    return new MapBasedInputRow(timestamp.getMillis(), dimensions, theMap);
  }

  @JsonProperty
  public TimestampSpec getTimestampSpec()
  {
    return timestampSpec;
  }

  @JsonProperty("data")
  public DataSpec getDataSpec()
  {
    return dataSpec;
  }

  @JsonProperty
  public Set<String> getDimensionExclusions()
  {
    return dimensionExclusions;
  }
}
