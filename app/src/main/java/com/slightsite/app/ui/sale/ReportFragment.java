package com.slightsite.app.ui.sale;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.slightsite.app.R;
import com.slightsite.app.domain.AppController;
import com.slightsite.app.domain.CurrencyController;
import com.slightsite.app.domain.DateTimeStrategy;
import com.slightsite.app.domain.customer.Customer;
import com.slightsite.app.domain.inventory.Inventory;
import com.slightsite.app.domain.inventory.LineItem;
import com.slightsite.app.domain.inventory.Product;
import com.slightsite.app.domain.inventory.ProductCatalog;
import com.slightsite.app.domain.params.ParamCatalog;
import com.slightsite.app.domain.params.ParamService;
import com.slightsite.app.domain.params.Params;
import com.slightsite.app.domain.payment.Payment;
import com.slightsite.app.domain.payment.PaymentCatalog;
import com.slightsite.app.domain.payment.PaymentService;
import com.slightsite.app.domain.sale.Sale;
import com.slightsite.app.domain.sale.SaleLedger;
import com.slightsite.app.domain.sale.Shipping;
import com.slightsite.app.techicalservices.NoDaoSetException;
import com.slightsite.app.techicalservices.Server;
import com.slightsite.app.techicalservices.Tools;
import com.slightsite.app.ui.MainActivity;
import com.slightsite.app.ui.component.UpdatableFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * UI for showing sale's record.
 *
 */
public class ReportFragment extends UpdatableFragment {
	
	private SaleLedger saleLedger;
	List<Map<String, String>> saleList;
	private ListView saleLedgerListView;
	private RecyclerView lineitemListRecycle;
	private RecyclerView lineitemListRecycle2;
	private TextView totalBox;
	private Spinner spinner;
	private Button previousButton;
	private Button nextButton;
	private TextView currentBox;
	private Calendar currentTime;
	private DatePickerDialog datePicker;
	private EditText searchBox;
	private SimpleAdapter sAdap;

	private PaymentCatalog paymentCatalog;
	private ParamCatalog paramCatalog;
	private ProductCatalog productCatalog;
	private int warehouse_id;
	private Boolean has_been_filtered = false;

	public static final int CUSTOM = 0;
	public static final int DAILY = 1;
	public static final int WEEKLY = 2;
	public static final int MONTHLY = 3;
	public static final int YEARLY = 4;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		try {
			saleLedger = SaleLedger.getInstance();
			paymentCatalog = PaymentService.getInstance().getPaymentCatalog();
			paramCatalog = ParamService.getInstance().getParamCatalog();
			Params pWarehouseId = paramCatalog.getParamByName("warehouse_id");
			if (pWarehouseId instanceof Params) {
				warehouse_id = Integer.parseInt(pWarehouseId.getValue());
			}
			productCatalog = Inventory.getInstance().getProductCatalog();
		} catch (NoDaoSetException e) {
			e.printStackTrace();
		}

		View view = inflater.inflate(R.layout.layout_report, container, false);
		setHasOptionsMenu(true);
		
		previousButton = (Button) view.findViewById(R.id.previousButton);
		nextButton = (Button) view.findViewById(R.id.nextButton);
		currentBox = (TextView) view.findViewById(R.id.currentBox);
		saleLedgerListView = (ListView) view.findViewById(R.id.saleListView);
		totalBox = (TextView) view.findViewById(R.id.totalBox);
		spinner = (Spinner) view.findViewById(R.id.spinner1);
		searchBox = (EditText) view.findViewById(R.id.searchBox);

		lineitemListRecycle = (RecyclerView) view.findViewById(R.id.lineitemListRecycle);
		lineitemListRecycle.setLayoutManager(new LinearLayoutManager(getContext()));
		lineitemListRecycle.setHasFixedSize(true);
		lineitemListRecycle.setNestedScrollingEnabled(false);

		lineitemListRecycle2 = (RecyclerView) view.findViewById(R.id.lineitemListRecycle2);
		lineitemListRecycle2.setLayoutManager(new LinearLayoutManager(getContext()));
		lineitemListRecycle2.setHasFixedSize(true);
		lineitemListRecycle2.setNestedScrollingEnabled(false);
		
		initUI();

		// search trigger
		searchBox.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
				// When user changed the Text
				//sAdap.getFilter().filter(cs);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
										  int arg3) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable arg0) {
				// TODO Auto-generated method stub
			}
		});

		return view;
	}

	/**
	 * Initiate this UI.
	 */
	private void initUI() {
		currentTime = Calendar.getInstance();
		datePicker = new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int y, int m, int d) {
				currentTime.set(Calendar.YEAR, y);
				currentTime.set(Calendar.MONTH, m);
				currentTime.set(Calendar.DAY_OF_MONTH, d);
				update();
			}
		}, currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH), currentTime.get(Calendar.DAY_OF_MONTH));
		
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity().getBaseContext(),
		        R.array.period, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		
		spinner.setAdapter(adapter);
		spinner.setSelection(0);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {	
				update();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) { }
			
		});
		
		currentBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				datePicker.show();
			}
		});
		
		
		
		previousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addDate(-1);
			}
		});
		
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addDate(1);
			}
		});

		saleLedgerListView.setOnItemClickListener(new OnItemClickListener() {
		      public void onItemClick(AdapterView<?> myAdapter, View myView, int position, long mylng) {
		    	  String id = saleList.get(position).get("id").toString();
		    	  Intent newActivity = new Intent(getActivity().getBaseContext(), SaleDetailActivity.class);
		          newActivity.putExtra("id", id);
		          startActivity(newActivity);  
		      }     
		});

		update();
	}
	
	/**
	 * Show list.
	 * @param list
	 */
	private void showList(List<Sale> list) {
		saleList = new ArrayList<Map<String, String>>();
		for (Sale sale : list) {
			Map<String, String> salemap = sale.toMap();
			salemap.put("customer_data", "-");
			try {
				Customer cust = saleLedger.getCustomerBySaleId(sale.getId());
				if (cust.getName().length() > 0) {
					salemap.put("customer_data", cust.getName() + " - " + cust.getPhone() + " - " + cust.getAddress());
				}
				Double tot = sale.getTotal() - sale.getDiscount();
				List<Payment> the_payments = paymentCatalog.getPaymentBySaleId(sale.getId());
				Double tot_payment = 0.0;
				if (the_payments != null) {
					tot_payment = paymentCatalog.getTotalPaymentBySaleId(sale.getId());
				}
				Log.e(getTag(), "discount : "+ sale.getDiscount());
				Log.e(getTag(), "tot : "+ tot +" and tot_payment : "+ tot_payment);
				Log.e(getTag(), "status : "+ sale.getStatus());
				if (tot_payment >= tot) {
					salemap.put("status", getResources().getString(R.string.message_paid));
					salemap.put("is_paid", "1");
				} else {
					salemap.put("status", getResources().getString(R.string.message_unpaid));
					salemap.put("is_paid", "0");
				}
			} catch (Exception e){
				e.printStackTrace();
			}

			saleList.add(salemap);
		}

		/*sAdap = new SimpleAdapter(getActivity().getBaseContext() , saleList,
				R.layout.listview_report_v2, new String[] { "id", "startTime", "total", "invoiceNumber", "customer_data", "status"},
				new int[] { R.id.sid, R.id.startTime , R.id.total, R.id.invoice_number, R.id.customer_data, R.id.status});
		saleLedgerListView.setAdapter(sAdap);*/

		AdapterListInvoice invAdapter = new AdapterListInvoice(getActivity().getBaseContext() , saleList);
		lineitemListRecycle.setAdapter(invAdapter);

		invAdapter.setOnItemClickListener(new AdapterListInvoice.OnItemClickListener() {
			@Override
			public void onItemClick(View view, Map<String, String> _item, int position) {
				String id = _item.get("id");
				Intent newActivity = new Intent(getActivity().getBaseContext(), SaleDetailActivity.class);
				newActivity.putExtra("id", id);
				startActivity(newActivity);
			}
		});
	}

	private void showList2(List<Sale> list) {
		saleList = new ArrayList<Map<String, String>>();
		for (Sale sale : list) {
			Map<String, String> salemap = sale.toMap();
			salemap.put("customer_data", "-");
			try {
				Customer cust = list_of_customers.get(sale.getCustomerId());
				if (cust.getName().length() > 0) {
					salemap.put("customer_data", cust.getName() + " - " + cust.getPhone() + " - " + cust.getAddress());
					salemap.put("customer_name", cust.getName());
					salemap.put("customer_phone", cust.getPhone());
					salemap.put("customer_address", cust.getAddress());
				}
				salemap.put("status", sale.getStatus());
				salemap.put("delivered_plan_at", sale.getDeliveredPlanAt());

				Shipping shipping = list_of_shippings.get(sale.getId());
				if (shipping != null) {
					Log.e(getTag(), "shipping : "+ shipping.toMap().toString());
					salemap.put("shipping_method", shipping.toMap().get("method_name"));
					if (shipping.toMap().containsKey("pickup_date") && !shipping.toMap().get("pickup_date").equals("null")) {
						salemap.put("delivered_plan_at", shipping.toMap().get("pickup_date"));
					}
				} else {
					salemap.put("shipping_method", "-");
				}
			} catch (Exception e){
				e.printStackTrace();
			}

			saleList.add(salemap);
		}

		AdapterListInvoice invAdapter = new AdapterListInvoice(getActivity().getBaseContext() , saleList);
		lineitemListRecycle.setAdapter(invAdapter);

		invAdapter.setOnItemClickListener(new AdapterListInvoice.OnItemClickListener() {
			@Override
			public void onItemClick(View view, Map<String, String> _item, int position) {
				String id = _item.get("id");
				Intent newActivity = new Intent(getActivity().getBaseContext(), SaleDetailActivity.class);
				newActivity.putExtra("id", id);
				Sale selected_sale = list_of_transactions.get(position);
				newActivity.putExtra("sale_intent", selected_sale);
				newActivity.putExtra("customer_intent", list_of_customers.get(selected_sale.getCustomerId()));
				newActivity.putExtra("shipping_intent", list_of_shippings.get(selected_sale.getId()));
				newActivity.putExtra("payment_intent", list_of_payments2.get(selected_sale.getId()).toString());
				newActivity.putExtra("line_items_intent", list_of_line_items2.get(selected_sale.getId()).toString());
				startActivity(newActivity);
			}
		});
	}

	@Override
	public void update() {
		int period = CUSTOM;
		if (spinner != null) {
			period = spinner.getSelectedItemPosition();
		}

		List<Sale> list = null;
		Calendar cTime = (Calendar) currentTime.clone();
		Calendar eTime = (Calendar) currentTime.clone();
		
		if(period == DAILY){
			currentBox.setText(" [" + DateTimeStrategy.getSQLDateFormat(currentTime) +  "] ");
			currentBox.setTextSize(16);
			eTime.add(Calendar.DATE, 1);
		} else if (period == WEEKLY){
			while(cTime.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY){
				cTime.add(Calendar.DATE, -1);
			}
			
			String toShow = " [" + DateTimeStrategy.getSQLDateFormat(cTime) +  "] ~ [";
			eTime = (Calendar) cTime.clone();
			eTime.add(Calendar.DATE, 7);
			toShow += DateTimeStrategy.getSQLDateFormat(eTime) +  "] ";
			currentBox.setTextSize(16);
			currentBox.setText(toShow);
		} else if (period == MONTHLY){
			cTime.set(Calendar.DATE, 1);
			eTime = (Calendar) cTime.clone();
			eTime.add(Calendar.MONTH, 1);
			eTime.add(Calendar.DATE, -1);
			currentBox.setTextSize(18);
			currentBox.setText(" [" + currentTime.get(Calendar.YEAR) + "-" + (currentTime.get(Calendar.MONTH)+1) + "] ");
		} else if (period == YEARLY){
			cTime.set(Calendar.DATE, 1);
			cTime.set(Calendar.MONTH, 0);
			eTime = (Calendar) cTime.clone();
			eTime.add(Calendar.YEAR, 1);
			eTime.add(Calendar.DATE, -1);
			currentBox.setTextSize(20);
			currentBox.setText(" [" + currentTime.get(Calendar.YEAR) +  "] ");
		} else if (period == CUSTOM){
			String toShow = " [" + DateTimeStrategy.getSQLDateFormat(cTime) +  "] ~ [";
			eTime = (Calendar) cTime.clone();
			eTime.add(Calendar.DATE, 30);
			toShow += DateTimeStrategy.getSQLDateFormat(eTime) +  "] ";
			currentBox.setTextSize(16);
			currentBox.setText(toShow);
		}
		currentTime = cTime;
		//list = saleLedger.getAllSaleDuring(cTime, eTime);
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("warehouse_id", warehouse_id+"");
			if (filter_result.size() > 0) {
				if (filter_result.containsKey("status")) {
					params.put("status_order", filter_result.get("status"));
				} else {
					params.put("status_order", "-");
				}

				if (filter_result.containsKey("customer_name")) {
					params.put("customer_name", filter_result.get("customer_name"));
				}

				if (filter_result.containsKey("customer_phone")) {
					params.put("customer_phone", filter_result.get("customer_phone"));
				}

				if (filter_result.containsKey("invoice_number")) {
					params.put("invoice_number", filter_result.get("invoice_number"));
				}

				if (filter_result.containsKey("shipping_method")) {
					String the_shipping_method = filter_result.get("shipping_method");
					if (the_shipping_method == null) {
						the_shipping_method = "0";
					}
					params.put("shipping_method", the_shipping_method);
				}
			} else {
				params.put("status_order", "-");
			}
			//params.put("created_at_from", DateTimeStrategy.getSQLDateFormat(cTime));
			//params.put("created_at_to", DateTimeStrategy.getSQLDateFormat(eTime));

			if (!filter_result.containsKey("date_from")) {
				filter_result.put("date_from", DateTimeStrategy.getSQLDateFormat(cTime));
			}

			if (!filter_result.containsKey("date_to")) {
				filter_result.put("date_to", DateTimeStrategy.getSQLDateFormat(eTime));
			}

			params.put("delivered_plan_at_from", filter_result.get("date_from"));
			params.put("delivered_plan_at_to", filter_result.get("date_to"));
			params.put("custom_order_by", "status_order_code");
			params.put("order_type", "ASC");

			Log.e(getTag(), "params : "+ params.toString());
			setTransactionList(params);
		} catch (Exception e){e.printStackTrace();}

		try {
			if (!has_been_filtered) {
				Map<String, String> params2 = new HashMap<String, String>();
				params2.put("warehouse_id", warehouse_id + "");
				params2.put("delivered", "0");
				params2.put("status_order", "lunas");
				params2.put("custom_order_by", "status_order_code");
				params2.put("order_type", "ASC");

				setTransactionList2(params2);
			}
		} catch (Exception e){e.printStackTrace();}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		// update();
		// it shouldn't call update() anymore. Because super.onResume() 
		// already fired the action of spinner.onItemSelected()
	}
	
	/**
	 * Add date.
	 * @param increment
	 */
	private void addDate(int increment) {
		int period = spinner.getSelectedItemPosition();
		if (period == DAILY){
			currentTime.add(Calendar.DATE, 1 * increment);
		} else if (period == WEEKLY){
			currentTime.add(Calendar.DATE, 7 * increment);
		} else if (period == MONTHLY){
			currentTime.add(Calendar.MONTH, 1 * increment);
		} else if (period == YEARLY){
			currentTime.add(Calendar.YEAR, 1 * increment);
		}
		update();
	}

	private List<Sale> list_of_transactions = new ArrayList<Sale>();
	private Map<Integer, Customer> list_of_customers = new HashMap<Integer, Customer>();
	private Map<Integer, Shipping> list_of_shippings = new HashMap<Integer, Shipping>();
	private JSONArray list_of_payments;
	private JSONArray list_of_line_items;
	private Map<Integer, JSONArray> list_of_payments2 = new HashMap<Integer, JSONArray>();
	private Map<Integer, JSONArray> list_of_line_items2 = new HashMap<Integer, JSONArray>();

	public void setTransactionList(final Map<String, String> params) {
		list_of_transactions.clear();
		list_of_customers.clear();
		list_of_shippings.clear();
		list_of_payments2.clear();
		list_of_line_items2.clear();
		String url = Server.URL + "transaction/list?api-key=" + Server.API_KEY;
		_string_request(Request.Method.GET, url, params, false,
				new VolleyCallback() {
					@Override
					public void onSuccess(String result) {
						try {
							Log.e(getClass().getSimpleName(), "result : "+ result);
							if (result.contains("success")) {
								JSONObject jObj = new JSONObject(result);
								int success = jObj.getInt("success");
								// Check for error node in json
								if (success == 1) {
									JSONArray data = jObj.getJSONArray("data");
									for(int n = 0; n < data.length(); n++) {
										JSONObject data_n = data.getJSONObject(n);
										Sale sale = new Sale(data_n.getInt("id"), data_n.getString("created_at"));
										sale.setServerInvoiceNumber(data_n.getString("invoice_number"));
										sale.setServerInvoiceId(data_n.getInt("id"));
										sale.setCustomerId(data_n.getInt("customer_id"));
										sale.setStatus(data_n.getString("status_order"));
										sale.setDeliveredPlanAt(data_n.getString("delivered_plan_at"));

										JSONObject config = data_n.getJSONObject("config");
										sale.setDiscount(config.getInt("discount"));

										list_of_transactions.add(sale);
										Customer cust = new Customer(
												sale.getCustomerId(),
												data_n.getString("customer_name"),
												data_n.getString("customer_email"),
												data_n.getString("customer_phone"),
												data_n.getString("customer_address"),
												data_n.getInt("customer_status")
										);
										list_of_customers.put(sale.getCustomerId(), cust);

										// build the shipping
										JSONArray arrShip = config.getJSONArray("shipping");
										if (arrShip.length() > 0) {
											JSONObject ship_method = arrShip.getJSONObject(0);
											if (ship_method != null) {
												Shipping shipping = new Shipping(
														ship_method.getInt("method"),
														ship_method.getString("date_added"),
														ship_method.getString("address"),
														ship_method.getInt("warehouse_id")
												);
												if (ship_method.has("warehouse_name")) {
													shipping.setWarehouseName(ship_method.getString("warehouse_name"));
												}
												if (ship_method.has("recipient_name")) {
													shipping.setName(ship_method.getString("recipient_name"));
												}
												if (ship_method.has("recipient_phone")) {
													shipping.setPhone(ship_method.getString("recipient_phone"));
												}
												if (ship_method.has("pickup_date")) {
													shipping.setPickupDate(ship_method.getString("pickup_date"));
												}
												list_of_shippings.put(sale.getId(), shipping);
											}
										}

										// build the payment
										JSONArray arrPayment = config.getJSONArray("payment");
										if (arrPayment.length() > 0) {
											list_of_payments = arrPayment; // salah banget
											list_of_payments2.put(sale.getId(), arrPayment);
										}

										// build the line item data
										JSONArray arrItemsBelanja = config.getJSONArray("items_belanja");
										if (arrItemsBelanja.length() > 0) {
											list_of_line_items = arrItemsBelanja;
											list_of_line_items2.put(sale.getId(), arrItemsBelanja);
										}
									}

									double total = 0;
									for (Sale sale : list_of_transactions)
										total += sale.getTotal();

									String total_formated = CurrencyController.getInstance().moneyFormat(total);
									totalBox.setText(total_formated);
									showList2(list_of_transactions);
								}
							} else {
								Toast.makeText(getContext(), "Failed!, No product data in ",
										Toast.LENGTH_LONG).show();
							}

						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});
	}

	private void _string_request(int method, String url, final Map params, final Boolean show_dialog, final VolleyCallback callback) {

		if (method == Request.Method.GET) { //get method doesnt support getParams
			Iterator<Map.Entry<String, String>> iterator = params.entrySet().iterator();
			while(iterator.hasNext())
			{
				Map.Entry<String, String> pair = iterator.next();
				String pair_value = pair.getValue();
				if (pair_value.contains(" "))
					pair_value = pair.getValue().replace(" ", "%20");
				url += "&" + pair.getKey() + "=" + pair_value;
			}
		}

		StringRequest strReq = new StringRequest(method, url, new Response.Listener < String > () {

			@Override
			public void onResponse(String Response) {
				callback.onSuccess(Response);
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				error.printStackTrace();
				Log.e(getClass().getSimpleName(), "ada error : "+ error.getMessage());
				Toast.makeText(getContext(),
						error.getMessage(), Toast.LENGTH_LONG).show();
			}
		})
		{
			// set headers
			@Override
			protected Map<String, String> getParams() {
				return params;
			}
		};

		strReq.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0,
				DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

		try {
			AppController.getInstance().addToRequestQueue(strReq, "json_obj_req");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public interface VolleyCallback {
		void onSuccess(String result);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_filter, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.nav_filter :
				filterDialog();
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private BottomSheetDialog bottomSheetDialog;
	private EditText filter_invoice_number;
	private EditText filter_customer_name;
	private EditText filter_customer_phone;
	private Spinner filter_status;
	private Button finish_submit_button;
	private Map<String, String> inv_status_map = new HashMap<String, String>();
	private Map<String, String> inv_status_map_keys = new HashMap<String, String>();
	private ArrayList<String> inv_status_items = new ArrayList<String>();
	private Map<String, String> filter_result = new HashMap<String, String>();
	private AutoCompleteTextView filter_date_from;
	private AutoCompleteTextView filter_date_to;
	private Spinner filter_shipping_method;
	private ArrayList<String> shipping_method_items = new ArrayList<String>();
	private Map<String, String> shipping_method_map_keys = new HashMap<String, String>();

	public void filterDialog() {
		bottomSheetDialog = new BottomSheetDialog(getContext());
		View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_filter_report, null);
		bottomSheetDialog.setContentView(sheetView);

		filter_invoice_number = (EditText) sheetView.findViewById(R.id.invoice_number);
		filter_customer_name = (EditText) sheetView.findViewById(R.id.customer_name);
		filter_customer_phone = (EditText) sheetView.findViewById(R.id.customer_phone);
		filter_status = (Spinner) sheetView.findViewById(R.id.status);
		filter_shipping_method = (Spinner) sheetView.findViewById(R.id.shipping_method);
		finish_submit_button = (Button) sheetView.findViewById(R.id.finish_submit_button);

		filter_date_from = (AutoCompleteTextView) sheetView.findViewById(R.id.date_from);
		filter_date_to = (AutoCompleteTextView) sheetView.findViewById(R.id.date_to);
		if (filter_result.containsKey("date_from")) {
			filter_date_from.setText(filter_result.get("date_from"));
		}
		if (filter_result.containsKey("date_to")) {
			filter_date_to.setText(filter_result.get("date_to"));
		}

		inv_status_map = Tools.getInvoiceStatusList();

		inv_status_items.clear();
		inv_status_map_keys.clear();
		filter_result.clear();
		for (Map.Entry<String, String> entry : inv_status_map.entrySet()) {
			inv_status_items.add(entry.getValue());
			inv_status_map_keys.put(entry.getValue(), entry.getKey());
		}

		ArrayAdapter<String> stAdapter = new ArrayAdapter<String>(
				getContext(),
				R.layout.spinner_item, inv_status_items);
		stAdapter.notifyDataSetChanged();
		filter_status.setAdapter(stAdapter);

		// build the shipping method list
		shipping_method_items.clear();
		shipping_method_map_keys.clear();
		shipping_method_items.add("Semua");
		String[] shipping_method_list = AppController.getPaymentMethods();
		int numberOfItems = shipping_method_list.length;
		for (int i=0; i < numberOfItems; i++)
		{
			String name = shipping_method_list[i];
			shipping_method_items.add(name);
			shipping_method_map_keys.put(name, i+"");
		}
		ArrayAdapter<String> spAdapter = new ArrayAdapter<String>(
				getContext(),
				R.layout.spinner_item, shipping_method_items);
		spAdapter.notifyDataSetChanged();
		filter_shipping_method.setAdapter(spAdapter);

		bottomSheetDialog.show();

		triggerBottomDialogButton(sheetView);
	}

	private void triggerBottomDialogButton(View view) {
		finish_submit_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				has_been_filtered = true;
				lineitemListRecycle2.setVisibility(View.GONE);

				bottomSheetDialog.dismiss();
				String status = filter_status.getSelectedItem().toString();
				if (status.length() > 0 && !status.equals("-")) {
					status = inv_status_map_keys.get(status);
					filter_result.put("status", status);
				}

				String ship_method = filter_shipping_method.getSelectedItem().toString();
				Log.e(getClass().getSimpleName(), "ship_method : "+ ship_method);
				Log.e(getClass().getSimpleName(), "shipping_method_map_keys : "+ shipping_method_map_keys.toString());
				if (ship_method.length() > 0) {
					if (shipping_method_map_keys.containsKey(ship_method)) {
						ship_method = shipping_method_map_keys.get(ship_method);
						if (ship_method != null) {
							int ship_method_int = Integer.parseInt(ship_method) + 1;
							ship_method = ship_method_int + "";
						}
					} else {
						ship_method = "0";
					}
                    Log.e(getClass().getSimpleName(), "final ship_method : "+ ship_method);
					filter_result.put("shipping_method", ship_method);
				}

				String invoice_number = filter_invoice_number.getText().toString();
				if (invoice_number.length() > 0) {
					filter_result.put("invoice_number", invoice_number);
				}

				String customer_name = filter_customer_name.getText().toString();
				if (customer_name.length() > 0) {
					filter_result.put("customer_name", customer_name);
				}

				String customer_phone = filter_customer_phone.getText().toString();
				if (customer_phone.length() > 0) {
					filter_result.put("customer_phone", customer_phone);
				}

				update();
			}
		});

		filter_date_from.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialogDatePickerLight(v, "date_from");
			}
		});

		filter_date_to.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dialogDatePickerLight(v, "date_to");
			}
		});
	}

	private void dialogDatePickerLight(final View v, final String fiter_name) {
		final Calendar cur_calender = Calendar.getInstance();

		DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(),
				new DatePickerDialog.OnDateSetListener() {

					@Override
					public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

						Calendar newDate = Calendar.getInstance();
						newDate.set(year, monthOfYear, dayOfMonth);
						long date = newDate.getTimeInMillis();
						((EditText) v).setText(Tools.getFormattedDateShort(date));
						filter_result.put(fiter_name, Tools.getFormattedDateFlat(date));

					}

				}, cur_calender.get(Calendar.YEAR), cur_calender.get(Calendar.MONTH), cur_calender.get(Calendar.DAY_OF_MONTH));

		datePickerDialog.show();
	}

	private List<Sale> list_of_transactions2 = new ArrayList<Sale>();
	private Map<Integer, Customer> list_of_customers2 = new HashMap<Integer, Customer>();
	private Map<Integer, Shipping> list_of_shippings2 = new HashMap<Integer, Shipping>();
	private Map<Integer, JSONArray> list_of_payments3 = new HashMap<Integer, JSONArray>();
	private Map<Integer, JSONArray> list_of_line_items3 = new HashMap<Integer, JSONArray>();

	public void setTransactionList2(final Map<String, String> params) {
		list_of_transactions2.clear();
		list_of_customers2.clear();
		list_of_shippings2.clear();
		list_of_payments3.clear();
		list_of_line_items3.clear();
		String url = Server.URL + "transaction/list?api-key=" + Server.API_KEY;
		_string_request(Request.Method.GET, url, params, false,
				new VolleyCallback() {
					@Override
					public void onSuccess(String result) {
						try {
							Log.e(getClass().getSimpleName(), "result old data : "+ result);
							if (result.contains("success")) {
								JSONObject jObj = new JSONObject(result);
								int success = jObj.getInt("success");
								// Check for error node in json
								if (success == 1) {
									JSONArray data = jObj.getJSONArray("data");
									for(int n = 0; n < data.length(); n++) {
										JSONObject data_n = data.getJSONObject(n);
										Sale sale = new Sale(data_n.getInt("id"), data_n.getString("created_at"));
										sale.setServerInvoiceNumber(data_n.getString("invoice_number"));
										sale.setServerInvoiceId(data_n.getInt("id"));
										sale.setCustomerId(data_n.getInt("customer_id"));
										sale.setStatus(data_n.getString("status_order"));
										sale.setDeliveredPlanAt(data_n.getString("delivered_plan_at"));

										JSONObject config = data_n.getJSONObject("config");
										sale.setDiscount(config.getInt("discount"));

										list_of_transactions2.add(sale);
										Customer cust = new Customer(
												sale.getCustomerId(),
												data_n.getString("customer_name"),
												data_n.getString("customer_email"),
												data_n.getString("customer_phone"),
												data_n.getString("customer_address"),
												data_n.getInt("customer_status")
										);
										list_of_customers2.put(sale.getCustomerId(), cust);

										// build the shipping
										JSONArray arrShip = config.getJSONArray("shipping");
										if (arrShip.length() > 0) {
											JSONObject ship_method = arrShip.getJSONObject(0);
											if (ship_method != null) {
												Shipping shipping = new Shipping(
														ship_method.getInt("method"),
														ship_method.getString("date_added"),
														ship_method.getString("address"),
														ship_method.getInt("warehouse_id")
												);
												if (ship_method.has("warehouse_name")) {
													shipping.setWarehouseName(ship_method.getString("warehouse_name"));
												}
												if (ship_method.has("recipient_name")) {
													shipping.setName(ship_method.getString("recipient_name"));
												}
												if (ship_method.has("recipient_phone")) {
													shipping.setPhone(ship_method.getString("recipient_phone"));
												}
												if (ship_method.has("pickup_date")) {
													shipping.setPickupDate(ship_method.getString("pickup_date"));
												}
												list_of_shippings2.put(sale.getId(), shipping);
											}
										}

										// build the payment
										JSONArray arrPayment = config.getJSONArray("payment");
										if (arrPayment.length() > 0) {
											list_of_payments3.put(sale.getId(), arrPayment);
										}

										// build the line item data
										JSONArray arrItemsBelanja = config.getJSONArray("items_belanja");
										if (arrItemsBelanja.length() > 0) {
											list_of_line_items3.put(sale.getId(), arrItemsBelanja);
										}
									}

									double total = 0;
									for (Sale sale : list_of_transactions2)
										total += sale.getTotal();

									String total_formated = CurrencyController.getInstance().moneyFormat(total);
									totalBox.setText(total_formated);
									showList3(list_of_transactions2);
								}
							} else {
								Toast.makeText(getContext(), "Failed!, No product data in ",
										Toast.LENGTH_LONG).show();
							}

						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				});
	}

	List<Map<String, String>> saleList2;

	private void showList3(List<Sale> list) {
		saleList2 = new ArrayList<Map<String, String>>();
		for (Sale sale : list) {
			Map<String, String> salemap = sale.toMap();
			salemap.put("customer_data", "-");
			try {
				Customer cust = list_of_customers2.get(sale.getCustomerId());
				if (cust.getName().length() > 0) {
					salemap.put("customer_data", cust.getName() + " - " + cust.getPhone() + " - " + cust.getAddress());
					salemap.put("customer_name", cust.getName());
					salemap.put("customer_phone", cust.getPhone());
					salemap.put("customer_address", cust.getAddress());
				}
				salemap.put("status", sale.getStatus());
				salemap.put("delivered_plan_at", sale.getDeliveredPlanAt());

				Shipping shipping = list_of_shippings2.get(sale.getId());
				if (shipping != null) {
					Log.e(getTag(), "shipping : "+ shipping.toMap().toString());
					salemap.put("shipping_method", shipping.toMap().get("method_name"));
					if (shipping.toMap().containsKey("pickup_date") && !shipping.toMap().get("pickup_date").equals("null")) {
						salemap.put("delivered_plan_at", shipping.toMap().get("pickup_date"));
					}
				} else {
					salemap.put("shipping_method", "-");
				}
			} catch (Exception e){
				e.printStackTrace();
			}

			saleList2.add(salemap);
		}

		AdapterListInvoice invAdapter = new AdapterListInvoice(getActivity().getBaseContext() , saleList2);
		lineitemListRecycle2.setAdapter(invAdapter);

		invAdapter.setOnItemClickListener(new AdapterListInvoice.OnItemClickListener() {
			@Override
			public void onItemClick(View view, Map<String, String> _item, int position) {
				String id = _item.get("id");
				Intent newActivity = new Intent(getActivity().getBaseContext(), SaleDetailActivity.class);
				newActivity.putExtra("id", id);
				Sale selected_sale = list_of_transactions2.get(position);
				newActivity.putExtra("sale_intent", selected_sale);
				newActivity.putExtra("customer_intent", list_of_customers2.get(selected_sale.getCustomerId()));
				newActivity.putExtra("shipping_intent", list_of_shippings2.get(selected_sale.getId()));
				newActivity.putExtra("payment_intent", list_of_payments3.get(selected_sale.getId()).toString());
				newActivity.putExtra("line_items_intent", list_of_line_items3.get(selected_sale.getId()).toString());
				startActivity(newActivity);
			}
		});
	}
}
