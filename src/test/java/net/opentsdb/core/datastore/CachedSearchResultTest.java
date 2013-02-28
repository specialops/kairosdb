// OpenTSDB2
// Copyright (C) 2013 Proofpoint, Inc.
//
// This program is free software: you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 2.1 of the License, or (at your
// option) any later version.  This program is distributed in the hope that it
// will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
// General Public License for more details.  You should have received a copy
// of the GNU Lesser General Public License along with this program.  If not,
// see <http://www.gnu.org/licenses/>

package net.opentsdb.core.datastore;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;
import net.opentsdb.core.DataPoint;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CachedSearchResultTest
{
	@Test
	public void test_createCachedSearchResult() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile";
		CachedSearchResult csResult =
				CachedSearchResult.createCachedSearchResult("metric1", tempFile);

		long now = System.currentTimeMillis();

		Map<String, String> tags = new HashMap();
		tags.put("host", "A");
		tags.put("client", "foo");
		csResult.startDataPointSet(tags);

		csResult.addDataPoint(now, 42);
		csResult.addDataPoint(now+1, 42.1);
		csResult.addDataPoint(now+2, 43);
		csResult.addDataPoint(now+3, 43.1);


		tags = new HashMap();
		tags.put("host", "B");
		tags.put("client", "foo");
		csResult.startDataPointSet(tags);

		csResult.addDataPoint(now, 1);
		csResult.addDataPoint(now+1, 1.1);
		csResult.addDataPoint(now+2, 2);
		csResult.addDataPoint(now+3, 2.1);

		tags = new HashMap();
		tags.put("host", "A");
		tags.put("client", "bar");
		csResult.startDataPointSet(tags);

		csResult.addDataPoint(now, 3);
		csResult.addDataPoint(now+1, 3.1);
		csResult.addDataPoint(now+2, 4);
		csResult.addDataPoint(now+3, 4.1);

		csResult.endDataPoints();

		List<DataPointRow> rows = csResult.getRows();

		assertEquals(3, rows.size());

		assertValues(rows.get(0), 42L, 42.1, 43L, 43.1);

		assertValues(rows.get(1), 1L, 1.1, 2L, 2.1);

		assertValues(rows.get(2), 3L, 3.1, 4L, 4.1);
	}

	@Test
	public void test_AddLongsBeyondBufferSize() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile";
		CachedSearchResult csResult = CachedSearchResult.createCachedSearchResult("metric2", tempFile);

		int numberOfDataPoints = CachedSearchResult.WRITE_BUFFER_SIZE * 2;
		csResult.startDataPointSet(Collections.<String, String>emptyMap());

		long now = System.currentTimeMillis();
		for (int i = 0; i < numberOfDataPoints; i++)
		{
			csResult.addDataPoint(now, 42);
		}

		csResult.endDataPoints();

		List<DataPointRow> rows = csResult.getRows();
		DataPointRow taggedDataPoints = rows.iterator().next();

		int count = 0;
		while(taggedDataPoints.hasNext())
		{
			DataPoint dataPoint = taggedDataPoints.next();
			assertThat(dataPoint.getLongValue(), equalTo(42L));
			count++;
		}

		assertThat(count, equalTo(numberOfDataPoints));

	}

	@Test
	public void test_AddDoublesBeyondBufferSize() throws IOException
	{
		String tempFile = System.getProperty("java.io.tmpdir") + "/baseFile";
		CachedSearchResult csResult = CachedSearchResult.createCachedSearchResult("metric3", tempFile);

		int numberOfDataPoints = CachedSearchResult.WRITE_BUFFER_SIZE * 2;
		csResult.startDataPointSet(Collections.<String, String>emptyMap());

		long now = System.currentTimeMillis();
		for (int i = 0; i < numberOfDataPoints; i++)
		{
			csResult.addDataPoint(now, 42.2);
		}

		csResult.endDataPoints();

		List<DataPointRow> rows = csResult.getRows();
		DataPointRow taggedDataPoints = rows.iterator().next();

		int count = 0;
		while(taggedDataPoints.hasNext())
		{
			DataPoint dataPoint = taggedDataPoints.next();
			assertThat(dataPoint.getDoubleValue(), equalTo(42.2));
			count++;
		}

		assertThat(count, equalTo(numberOfDataPoints));

	}

	private void assertValues(DataPointRow dataPoints, Number... numbers)
	{
		int count = 0;
		while (dataPoints.hasNext())
		{
			DataPoint dp = dataPoints.next();

			if (dp.isInteger())
			{
				Long value = (Long)numbers[count];
				assertEquals(value.longValue(), dp.getLongValue());
			}
			else
			{
				Double value = (Double)numbers[count];
				assertEquals(value.doubleValue(), dp.getDoubleValue());
			}

			count ++;
		}
	}


}
