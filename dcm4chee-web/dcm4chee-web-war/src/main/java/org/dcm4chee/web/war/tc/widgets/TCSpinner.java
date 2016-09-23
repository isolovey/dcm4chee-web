package org.dcm4chee.web.war.tc.widgets;

import java.io.Serializable;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.dcm4chee.web.war.tc.TCUtilities;
import org.dcm4chee.web.war.tc.TCUtilities.TCChangeListener;

public class TCSpinner<T extends Serializable> extends TextField<T> {
	private static final long serialVersionUID = 2162531566141823952L;

	protected AbstractAjaxBehavior valueChangeBehavior;
	
	private TCSpinner(final String id, final IModel<T> model, final TCChangeListener<T> l) {
		super(id, model);
		add(valueChangeBehavior = new AbstractDefaultAjaxBehavior() {
			private static final long serialVersionUID = -9003645162042802431L;
			@Override
			public void respond(AjaxRequestTarget target) {
				String stringValue = RequestCycle.get().getRequest().getParameter("value");
				TCSpinner.this.setModelValue(new String[] {stringValue});
				T value = TCSpinner.this.getModelObject();
				valueChanged(value);
				if (l!=null) {
					l.valueChanged(value);
				}
			}
		});
	}
	
	public static TCSpinner<Integer> createYearSpinner(final String id, TCChangeListener<Integer> l) {
		return createYearSpinner(id, (Integer)null, l);
	}
	
	public static TCSpinner<Integer> createYearSpinner(final String id, Integer year, TCChangeListener<Integer> l) {
		return createYearSpinner(id, new Model<Integer>(year), l);
	}
	
	public static TCSpinner<Integer> createYearSpinner(final String id, IModel<Integer> model, TCChangeListener<Integer> l) {
		final String syears = TCUtilities.getLocalizedString("tc.years.text");
		return new TCSpinner<Integer>(id, model, l) {
			private static final long serialVersionUID = 7945115163952095613L;
			@Override
			protected Integer convertValue(String[] values) throws ConversionException {
				try {
					if (values!=null && values.length>0) {
						String years = trim(values[0].replaceAll(syears, ""));
						if (years!=null && !years.isEmpty()) {
							return Integer.valueOf(years);
						}
					}
					return null;
				}
				catch (Exception e) {
					throw new ConversionException(e);
				}
			}
			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("readonly", "readonly");
				tag.put("loc-years", syears);
			}
		};
	}
	
	public static TCSpinner<Integer> createMonthSpinner(final String id, TCChangeListener<Integer> l) {
		return createMonthSpinner(id, (Integer)null, l);
	}
	
	public static TCSpinner<Integer> createMonthSpinner(final String id, Integer month, TCChangeListener<Integer> l) {
		return createMonthSpinner(id, new Model<Integer>(month), l);
	}
	
	public static TCSpinner<Integer> createMonthSpinner(final String id, IModel<Integer> model, TCChangeListener<Integer> l) {
		final String smonths = TCUtilities.getLocalizedString("tc.months.text");
		return new TCSpinner<Integer>(id, model, l) {
			private static final long serialVersionUID = 7945115163952095613L;
			@Override
			protected Integer convertValue(String[] values) throws ConversionException {
				try {
					if (values!=null && values.length>0) {
						String months = trim(values[0].replaceAll(smonths, ""));
						if (months!=null&&!months.isEmpty()) {
							return Integer.valueOf(months);
						}
					}
					return null;
				}
				catch (Exception e) {
					throw new ConversionException(e);
				}
			}
			@Override
			protected void onComponentTag(ComponentTag tag) {
				super.onComponentTag(tag);
				tag.put("readonly", "readonly");
				tag.put("loc-months", smonths);
			}
		};
	}

	protected void valueChanged(T value) {
		/* do nothing by default */
	}
	
	@Override
	protected void onComponentTag(ComponentTag tag) {
		super.onComponentTag(tag);
		tag.put("wicket-callback-url", valueChangeBehavior.getCallbackUrl());
	}
	
}
