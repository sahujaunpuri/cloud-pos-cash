package com.slightsite.app.ui.inventory;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.balysv.materialripple.MaterialRippleLayout;
import com.github.clans.fab.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentIntegratorSupportV4;
import com.google.zxing.integration.android.IntentResult;
import com.slightsite.app.R;
import com.slightsite.app.domain.CurrencyController;
import com.slightsite.app.domain.inventory.Inventory;
import com.slightsite.app.domain.inventory.LineItem;
import com.slightsite.app.domain.inventory.Product;
import com.slightsite.app.domain.inventory.ProductCatalog;
import com.slightsite.app.domain.sale.Register;
import com.slightsite.app.domain.sale.Sale;
import com.slightsite.app.domain.warehouse.Warehouses;
import com.slightsite.app.techicalservices.DatabaseExecutor;
import com.slightsite.app.techicalservices.Demo;
import com.slightsite.app.techicalservices.NoDaoSetException;
import com.slightsite.app.ui.MainActivity;
import com.slightsite.app.ui.component.ButtonAdapter;
import com.slightsite.app.ui.component.UpdatableFragment;
import com.slightsite.app.ui.sale.AdapterListProduct;
import com.slightsite.app.ui.sale.ChangeWarehouseDialogFragment;

/**
 * UI for Inventory, shows list of Product in the ProductCatalog.
 * Also use for a sale process of adding Product into sale.
 *
 *
 */
@SuppressLint("ValidFragment")
public class InventoryFragment extends UpdatableFragment {

	protected static final int SEARCH_LIMIT = 0;
	private GridView inventoryListView;
	private ProductCatalog productCatalog;
	private List<Map<String, String>> inventoryList;
	private com.github.clans.fab.FloatingActionButton addProductButton;
	private EditText searchBox;
	private Button scanButton;
	private FloatingActionButton syncProductButton;
	private TextView cart_total;
	private LinearLayout bottom_cart_container;
	private MaterialRippleLayout lyt_next;
	private LinearLayout no_product_container;

	private ViewPager viewPager;
	private Register register;
	private MainActivity main;

	private UpdatableFragment saleFragment;
	private Resources res;

	private Map<Integer, Integer> stacks = new HashMap<Integer, Integer>();

	private View fragment_view;

	/**
	 * Construct a new InventoryFragment.
	 * @param saleFragment
	 */
	public InventoryFragment(UpdatableFragment saleFragment) {
		super();
		this.saleFragment = saleFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		try {
			productCatalog = Inventory.getInstance().getProductCatalog();
			register = Register.getInstance();
		} catch (NoDaoSetException e) {
			e.printStackTrace();
		}

		View view = inflater.inflate(R.layout.layout_inventory, container, false);
		setHasOptionsMenu(true);

		fragment_view = view;

		res = getResources();
		inventoryListView = (GridView) view.findViewById(R.id.productListView);
		addProductButton = (com.github.clans.fab.FloatingActionButton) view.findViewById(R.id.addProductButton);
		scanButton = (Button) view.findViewById(R.id.scanButton);
		searchBox = (EditText) view.findViewById(R.id.searchBox);
		syncProductButton = (FloatingActionButton) view.findViewById(R.id.syncProductButton);
		cart_total = (TextView) view.findViewById(R.id.cart_total);
		bottom_cart_container = (LinearLayout) view.findViewById(R.id.bottom_cart_container);
		lyt_next = (MaterialRippleLayout) view.findViewById(R.id.lyt_next);
		no_product_container = (LinearLayout) view.findViewById(R.id.no_product_container);

		main = (MainActivity) getActivity();
		viewPager = main.getViewPager();

		initUI();
		updateCart();

		return view;
	}

	/**
	 * Initiate this UI.
	 */
	private void initUI() {

		addProductButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showPopup(v);
			}
		});

		searchBox.addTextChangedListener(new TextWatcher(){
			public void afterTextChanged(Editable s) {
				if (s.length() >= SEARCH_LIMIT) {
					search();
				}
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after){}
			public void onTextChanged(CharSequence s, int start, int before, int count){}
		});


		inventoryListView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> myAdapter, final View myView, int position, long mylng) {
				LinearLayout add_qty_container = (LinearLayout) myView.findViewById(R.id.add_qty_container);

				int id = Integer.parseInt(inventoryList.get(position).get("id").toString());
				if (!add_qty_container.isShown()) {

					register.addItem(productCatalog.getProductById(id), 1);
					saleFragment.update();
					viewPager.setCurrentItem(0);
					updateCart();

					add_qty_container.setVisibility(View.VISIBLE);
					myView.findViewById(R.id.optionView).setVisibility(View.GONE);
					TextView quantity = myView.findViewById(R.id.quantity);
					quantity.setText(""+ 1);

					/*Toast toast = Toast.makeText(
							getActivity().getApplicationContext(),
							productCatalog.getProductById(id).getName()+ ' ' +res.getString(R.string.message_success_added),
							Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 8);
					toast.show();*/
				}
			}     
		});

		scanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				IntentIntegratorSupportV4 scanIntegrator = new IntentIntegratorSupportV4(InventoryFragment.this);
				scanIntegrator.initiateScan();
			}
		});

		syncProductButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent newActivity = new Intent(getActivity(),
						ProductServerActivity.class);
				startActivity(newActivity);
			}
		});

		lyt_next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				viewPager.setCurrentItem(1);
			}
		});
	}

	/**
	 * Show list.
	 * @param list
	 */
	private void showList(List<Product> list) {

		inventoryList = new ArrayList<Map<String, String>>();
		for(Product product : list) {
			inventoryList.add(product.toMap());
			Log.e(getTag(), "product.toMap() : "+ product.toMap().toString());
		}

		if (inventoryList.size() == 0) {
			no_product_container.setVisibility(View.VISIBLE);
		} else {
			no_product_container.setVisibility(View.GONE);
		}

		// clearing the stack on update
		this.stacks = new HashMap<Integer, Integer>();

		AdapterListProduct pAdap = new AdapterListProduct(main, list, R.layout.listview_inventory, InventoryFragment.this);
		inventoryListView.setAdapter(pAdap);
	}

	/**
	 * Search.
	 */
	private void search() {
		if (searchBox == null) {
			return;
		}

		String search = searchBox.getText().toString();

		if (search.equals("/demo")) {
			testAddProduct();
			searchBox.setText("");
		} else if (search.equals("/clear")) {
			DatabaseExecutor.getInstance().dropAllData();
			searchBox.setText("");
		}
		else if (search.equals("")) {
			showList(productCatalog.getAllProduct());
		} else {
			List<Product> result = productCatalog.searchProduct(search);
			showList(result);
			if (result.isEmpty()) {

			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		IntentResult scanningResult = IntentIntegrator.parseActivityResult(
				requestCode, resultCode, intent);

		if (scanningResult != null) {
			String scanContent = scanningResult.getContents();
			searchBox.setText(scanContent);
		} else {
			Toast.makeText(getActivity().getBaseContext(), res.getString(R.string.fail),
					Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Test adding product
	 */
	protected void testAddProduct() {
		Demo.testProduct(getActivity());
		Toast.makeText(getActivity().getBaseContext(), res.getString(R.string.success),
				Toast.LENGTH_SHORT).show();
	}

	/**
	 * Show popup.
	 * @param anchorView
	 */
	public void showPopup(View anchorView) {
		AddProductDialogFragment newFragment = new AddProductDialogFragment(InventoryFragment.this);
		newFragment.show(getFragmentManager(), "");
	}

	@Override
	public void update() {
		search();
		updateCart();
	}

	@Override
	public void onResume() {
		super.onResume();
		update();
	}

	public void updateCart() {
		Double tot_cart = register.getTotal();
		if (tot_cart > 0) {
			Integer tot_item_cart = register.getCurrentSale().getAllLineItem().size();
			cart_total.setText("Sub Total " + CurrencyController.getInstance().moneyFormat(tot_cart) + " of " + tot_item_cart+ " Items");

			bottom_cart_container.setVisibility(View.VISIBLE);
		} else {
			bottom_cart_container.setVisibility(View.INVISIBLE);
			cart_total.setText("Empty cart");
		}
	}

	/*public void triggerAddSubstractButton(final View myView, final Integer id, final Integer position) {
		myView.findViewById(R.id.add_qty).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				TextView quantity = myView.findViewById(R.id.quantity);
				int the_qty = Integer.parseInt(quantity.getText().toString()) + 1;
				quantity.setText(""+ the_qty);
				register.addItem(productCatalog.getProductById(id), 1);
				updateCart();
				saleFragment.update();
				stacks.put(id, the_qty);
				Log.e(getTag(), "On adding qty "+ position);
			}
		});

		myView.findViewById(R.id.substract_qty).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.e(getTag(), "On substract qty");
				TextView quantity = myView.findViewById(R.id.quantity);
				int current_qty = Integer.parseInt(quantity.getText().toString());
				int the_qty = current_qty;
				if (current_qty > 0) {
					the_qty = current_qty - 1;
				}
				int id = Integer.parseInt(inventoryList.get(position).get("id").toString());

				if (the_qty == 0) {
					myView.findViewById(R.id.add_qty_container).setVisibility(View.GONE);
					//myView.findViewById(R.id.optionView).setVisibility(View.VISIBLE);
					myView.findViewById(R.id.addCartButton).setVisibility(View.VISIBLE);
					// substract the cart
					try {
						//LineItem lineItem = register.getCurrentSale().getLineItemAt(position);
						LineItem lineItem = register.getCurrentSale().getLineItemByProductId(id);
						if (lineItem != null && lineItem.getId() >= 0) {
							Log.e(getTag(), "LI : "+ lineItem.getProduct().getName());
							register.removeItem(lineItem);
							stacks.remove(id);
						} else {
							Log.e(getTag(), "position : "+ position);
						}
						updateCart();
					} catch (Exception e) {
						Log.e(getTag(), e.getMessage());
					}
				} else {
					LineItem lineItem = register.getCurrentSale().getLineItemByProductId(id);
					if (!lineItem.getProduct().equals(null)) {
						Double grosir_price = lineItem.getProduct().getUnitPriceByQuantity(lineItem.getProduct().getId(), the_qty);
						if (grosir_price > 0) {
							register.updateItem(
									register.getCurrentSale().getId(),
									lineItem,
									the_qty,
									grosir_price
							);
						} else {
							register.updateItem(
									register.getCurrentSale().getId(),
									lineItem,
									the_qty,
									lineItem.getProduct().getUnitPrice()
							);
						}
						stacks.put(id, the_qty);
					}
					updateCart();
				}
				quantity.setText(""+ the_qty);
				saleFragment.update();
			}
		});
	}*/

	public void addToCart(Product p) {
		register.addItem(p, 1);
		LineItem lineItem = register.getCurrentSale().getLineItemByProductId(p.getId());
		if (!lineItem.getProduct().equals(null)) {
			Integer tot_qty_in_cart = register.getCurrentSale().getOrders();
			Double grosir_price = lineItem.getProduct().getUnitPriceByQuantity(p.getId(), tot_qty_in_cart);
			if (grosir_price > 0) {
				register.updateItem(
						register.getCurrentSale().getId(),
						lineItem,
						1,
						grosir_price
				);
			}
		}
		stacks.put(p.getId(), 1);
		saleFragment.update();
		viewPager.setCurrentItem(0);
		updateCart();
	}

	public void addSubstractTheCart(Product p, int quantity) {
		if (quantity == 0) {
			// substract the cart
			try {
				LineItem lineItem = register.getCurrentSale().getLineItemByProductId(p.getId());
				if (lineItem != null && lineItem.getId() >= 0) {
					register.removeItem(lineItem);
					stacks.remove(p.getId());
				}
			} catch (Exception e) { e.printStackTrace();}
		} else {
			LineItem lineItem = register.getCurrentSale().getLineItemByProductId(p.getId());
			if (!lineItem.getProduct().equals(null)) {
				Integer tot_qty_in_cart = register.getCurrentSale().getOrders();
				Double grosir_price = lineItem.getProduct().getUnitPriceByQuantity(p.getId(), tot_qty_in_cart);
				if (grosir_price > 0) {
					register.updateItem(
							register.getCurrentSale().getId(),
							lineItem,
							quantity,
							grosir_price
					);
				} else {
					register.updateItem(
							register.getCurrentSale().getId(),
							lineItem,
							quantity,
							p.getUnitPrice()
					);
				}
				stacks.put(p.getId(), quantity);
			}
		}

		saleFragment.update();
		viewPager.setCurrentItem(0);
		updateCart();
	}

	public Map<Integer, Integer> getStacks() {
		for(LineItem line : register.getCurrentSale().getAllLineItem()){
			updateStack(line.getProduct().getId(), line.getQuantity());
		}

		return stacks;
	}

	public void updateStack(int product_id, int quantity) {
		if (quantity > 0) {
			stacks.put(product_id, quantity);
		} else {
			stacks.remove(product_id);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		List<Warehouses> whs = ((MainActivity)getActivity()).getWarehouseList();
		if (whs.size() > 0) {
			inflater.inflate(R.menu.menu_branch, menu);
			super.onCreateOptionsMenu(menu, inflater);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.nav_branch :
				// no confirm dialog
				changeWHPopup();
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void changeWHPopup() {
		ChangeWarehouseDialogFragment newFragment = new ChangeWarehouseDialogFragment(InventoryFragment.this);
		newFragment.show(getFragmentManager(), "");
	}
}