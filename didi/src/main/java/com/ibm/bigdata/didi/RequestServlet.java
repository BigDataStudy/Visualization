package com.ibm.bigdata.didi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import com.alibaba.fastjson.JSON;

/**
 * Servlet implementation class RequestServlet
 */
public class RequestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final TableName TABLE_NAME = TableName
			.valueOf("didi:order_statistics");

	/**
	 * Default constructor.
	 */
	public RequestServlet() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Connection to the cluster. A single connection shared by all
	 * application threads.
	 */
	Connection connection = null;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		Configuration con = new Configuration();
		con.set("hbase.zookeeper.quorum", "9.115.65.93");
		// establish the connection to the cluster.
		try {
			connection = ConnectionFactory.createConnection(con);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String district = request.getParameter("district");
		String date = request.getParameter("datett");
		String output;
		if (district == null || district == "") {
			district = "f9280c5dab6910ed44e518248048b9fe";
		}
		if (date == null || date == "") {
			List<String> dateList = getAWeekDates();
			List<Series> srList = new ArrayList<Series>();
			for(String dateStr : dateList){
				Result rs = getRsByDate(district, dateStr);
				Series sr = getSeries(rs, district, dateStr);
				if (sr != null){
					srList.add(sr);
				}
			}
			output = JSON.toJSONString(srList);
		} else {
			Result rs = getRsByDate(district, date);
			Series sr = getSeries(rs, district, date);
			output = JSON.toJSONString(sr);
		}
		response.getWriter().append(output);
		// response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	private Result getRsByDate(String district, String date)
			throws IOException {
		/**
		 * A lightweight handle to a specific table. Used from a single thread.
		 */
		Table table = null;
		try {
			if ( null == connection ||  connection.isClosed() ){
				this.init(null);
			}
			// retrieve a handle to the target table.
			table = connection.getTable(TABLE_NAME);
			 Get get = new
			 Get(Bytes.toBytes(district.concat("-").concat(date)));
			 Result rs = table.get(get);
			return rs;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// close everything down
			if (table != null)
				table.close();
		}
		return null;
	}
	
	
	private ResultScanner getRsByWeek(String district, List<String> dateList)
			throws IOException {
		/**
		 * A lightweight handle to a specific table. Used from a single thread.
		 */
		Table table = null;
		try {
			if ( null == connection ||  connection.isClosed() ){
				this.init(null);
			}
			table = connection.getTable(TABLE_NAME);
			byte[] startRow = Bytes.toBytes(district.concat("-").concat(
					dateList.get(0)));
			byte[] stopRow = Bytes.toBytes(district.concat("-").concat(
					dateList.get(dateList.size() - 1)));

			Scan scan = new Scan(startRow, stopRow);

			return table.getScanner(scan);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// close everything down
			if (table != null)
				table.close();
		}
		return null;
	}

	private List<Series> getSeriesList(ResultScanner rsc, String district) {
		List<Series> all = new ArrayList<Series>();
		if ( rsc.iterator() != null ){
			Iterator<Result> it = rsc.iterator(); 
			if (it.hasNext()) {
				getSeries(it.next(), district, "");
			}
		}
			
		return all;
	}

	private Series getSeries(Result rs, String district, String date) {
		if (rs != null && !rs.isEmpty()) {
			Series sr = new Series();
			sr.setDistrict(district);
			sr.setDate(date);
			for (int i = 1; i < 145; ++i) {
				String requestData = null, replyData = null;
				Cell cell = rs.getColumnLatestCell(
						Bytes.toBytes(String.valueOf(i)),
						Bytes.toBytes("request"));
				if (cell != null) {
					requestData = new String(CellUtil.cloneValue(cell));
				}
				cell = rs.getColumnLatestCell(Bytes.toBytes(String.valueOf(i)),
						Bytes.toBytes("reply"));
				if (cell != null) {
					replyData = new String(CellUtil.cloneValue(cell));
				}

				if (requestData != null || replyData != null) {
					Slot slot = new Slot();
					slot.setId(i);
					slot.setReply(replyData);
					slot.setRequest(requestData);
					sr.getData().add(slot);
				}
			}
			return sr;
		}
		return null;
	}

	private List<String> getAWeekDates() {
		List<String> curWeekDates = new ArrayList<String>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -6);
		for (int i = 0; i < 7; i++) {
			String dateStr = sdf.format(calendar.getTime());
			curWeekDates.add(dateStr);
			calendar.add(Calendar.DATE, 1);
		}

		return curWeekDates;
	}

}
