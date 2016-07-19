package com.ibm.bigdata.didi;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import com.alibaba.fastjson.JSON;

/**
 * Servlet implementation class RequestServlet
 */
public class RequestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final TableName TABLE_NAME = TableName.valueOf("didi:order_statistics");

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
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
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

			Get get = new Get(Bytes.toBytes("5725c39a5e5f4c188d382da3910b3f3f-2016-07-14"));
			Result rs= table.get(get);
			if (rs != null && !rs.isEmpty()) {
				Series sr= new Series();
				sr.setDistrict("5725c39a5e5f4c188d382da3910b3f3f");
				for (int i=1; i<145; ++i) {
					
					String requestData = null, replyData = null;
					Cell cell= rs.getColumnLatestCell(Bytes.toBytes(String.valueOf(i)), Bytes.toBytes("request"));
					if (cell != null) {
						requestData = new String(CellUtil.cloneValue(cell));
					}
					
					cell= rs.getColumnLatestCell(Bytes.toBytes(String.valueOf(i)), Bytes.toBytes("reply"));
					if (cell != null) {
						replyData = new String(CellUtil.cloneValue(cell));
					}
					
					if (requestData != null || replyData != null) {
						Slot slot= new Slot();
						slot.setId(i);
						slot.setReply(replyData);
						slot.setRequest(requestData);
						sr.getData().add(slot);
					}
					
				}
				
				String output= JSON.toJSONString(sr);
				response.getWriter().append(output);
			}
			

		} finally {
			// close everything down
			if (table != null)
				table.close();
			if (connection != null)
				connection.close();
		}

//		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
