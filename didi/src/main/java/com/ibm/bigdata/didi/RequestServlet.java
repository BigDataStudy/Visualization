package com.ibm.bigdata.didi;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.util.Bytes;

import com.alibaba.fastjson.JSON;

/**
 * Servlet implementation class RequestServlet
 */
public class RequestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final TableName TABLE_NAME = TableName
			.valueOf("didi:order_statistics");
	private static final int FIRST_DAY = Calendar.MONDAY;

	/**
	 * Default constructor.
	 */
	public RequestServlet() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		String district = request.getParameter("district");
		String date = request.getParameter("datett");
		Result rs = new Result();
		Series sr;
		if (district == null || district == "") {
			district = "f9280c5dab6910ed44e518248048b9fe";
		}
		if (date == null || date == "") {
			date = "2016-07-18";
			rs = getRsFromHBase(district, date);
			sr = getSeries(rs, district, date);
		} else {
			rs = getRsFromHBase(district, date);
			sr = getSeries(rs,district, date);
		}
		String output = JSON.toJSONString(sr);
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

	private Result getRsFromHBase(String district, String date)
			throws IOException {
		Configuration con = new Configuration();
		con.set("hbase.zookeeper.quorum", "9.115.65.93");

		/**
		 * Connection to the cluster. A single connection shared by all
		 * application threads.
		 */
		Connection connection = null;
		/**
		 * A lightweight handle to a specific table. Used from a single thread.
		 */
		Table table = null;
		try {
			// establish the connection to the cluster.
			connection = ConnectionFactory.createConnection(con);
			// retrieve a handle to the target table.
			table = connection.getTable(TABLE_NAME);
			Get get = new Get(Bytes.toBytes(district.concat("-").concat(date)));
			Result rs = table.get(get);
//			Result rs = new Result();
			List<String> weekList =  getCurWeekDates();
			
			
					byte []	startRow = Bytes.toBytes(district.concat("-").concat(weekList.get(0)));
			byte []	stopRow = Bytes.toBytes(district.concat("-").concat(weekList.get(weekList.size()-1)));
			
			Scan scan = new  Scan( startRow, stopRow);
			
			ResultScanner rsc = table.getScanner(scan);
			for ( Iterator<Result> it = rsc.iterator(); it.hasNext(); ){
				Result r = it.next();
//				System.out.println(new String(r.getRow()));
//				System.out.println(r);
			}
			
			return rs;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// close everything down
			if (table != null)
				table.close();
			if (connection != null)
				connection.close();
		}
		return null;
	}

	private Series getSeries(Result rs, String district, String date) {
		if (rs != null && !rs.isEmpty()) {
			Series sr = new Series();
			sr.setDistrict(district);
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
	
	private List<String> getCurWeekDates(){
		List<String> curWeekDates = new ArrayList<String>(); 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
		sdf.format(new Date());
		Calendar calendar = Calendar.getInstance();
		while (calendar.get(Calendar.DAY_OF_WEEK) != FIRST_DAY) {
			calendar.add(Calendar.DATE, -1);
		}
		for (int i = 0; i < 7; i++) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			String dateStr = dateFormat.format(calendar.getTime());
			curWeekDates.add(dateStr);
			calendar.add(Calendar.DATE, 1);
		}
		return curWeekDates;
	}

}
