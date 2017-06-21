package org.scijava.batch;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.scijava.Priority;
import org.scijava.convert.ConvertService;
import org.scijava.log.LogService;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptInfo;
import org.scijava.script.ScriptService;
import org.scijava.ui.swing.widget.SwingInputWidget;
import org.scijava.widget.InputWidget;
import org.scijava.widget.WidgetModel;

@Plugin(type = InputWidget.class, priority = Priority.NORMAL_PRIORITY)
public class SwingScriptInfoWidget extends SwingInputWidget<ScriptInfo>
		implements ActionListener, ScriptInfoWidget<JPanel> {

	@Parameter
	private ScriptService scripts;

	@Parameter
	private ModuleService modules;

	@Parameter
	private ConvertService convert;

	@Parameter
	private LogService log;

	private JComboBox<String> comboBox;

	private Map<String, ScriptInfo> scriptMap = new HashMap<>();

	// -- ActionListener methods --

	@Override
	public void actionPerformed(final ActionEvent e) {
		updateModel();
	}

	// -- WrapperPlugin methods --

	@Override
	public void set(final WidgetModel model) {
		super.set(model);

		// get required class from style attribute
		String style = model.getItem().getWidgetStyle();
		if (style == null) {
			style = "java.io.File"; // default to File
		}
		Class<?> inputType;
		try {
			inputType = Class.forName(style);
		} catch (ClassNotFoundException exc) {
			log.warn("Wrong style attribute: ", exc);
			inputType = File.class;
		}

		// create script map
		for (ScriptInfo script : scripts.getScripts()) {
			Module scriptModule = modules.createModule(script);
			for (String inputItem : scriptModule.getInputs().keySet()) {
				// TODO consider replacing by isAssignableFrom
				if (convert.supports(inputType, script.getInput(inputItem).getType())) {
				//if (script.getInput(inputItem).getType().isAssignableFrom(inputType)) {
					log.info("Support conversion from " + inputType + " to " + script.getInput(inputItem).getType());
					scriptMap.put(script.getMenuPath().getMenuString(), script);
					break;
				}
			}
		}

		final String[] items = scriptMap.keySet().toArray(new String[scriptMap.size()]);

		comboBox = new JComboBox<>(items);
		setToolTip(comboBox);
		getComponent().add(comboBox);
		comboBox.addActionListener(this);

		refreshWidget();
	}

	// -- InputWidget methods --

	@Override
	public ScriptInfo getValue() {
		return scriptMap.get(comboBox.getSelectedItem());
	}

	// -- AbstractUIInputWidget methods --

	@Override
	protected void doRefresh() {
		get().setValue(getValue()); // TODO check: should update widget, not model
	}

	@Override
	public boolean supports(final WidgetModel model) {
		return super.supports(model) && model.isType(ScriptInfo.class);
	}

}
