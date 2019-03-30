/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.preparation;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.ui.home.BaseNavigationContent;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.home.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "preparation", layout = OwlcmsRouterLayout.class)
public class PreparationNavigationContent extends BaseNavigationContent {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(PreparationNavigationContent.class);
	static { logger.setLevel(Level.DEBUG);}

	/**
	 * Instantiates a new preparation navigation content.
	 */
	public PreparationNavigationContent() {
		Button competition = new Button("Competition Information",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CompetitionContent.class));
		Button categories = new Button("Define Categories",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		Button groups = new Button("Define Groups",
				buttonClickEvent -> UI.getCurrent()
					.navigate(GroupContent.class));
		Button upload = new Button("Upload Registration File",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		Button athletes = new Button("Edit Athlete Entries",
				buttonClickEvent -> UI.getCurrent()
					.navigate(AthletesContent.class));
		Button weighIn = new Button("Weigh-In and Start Numbers",
			buttonClickEvent -> UI.getCurrent()
				.navigate(WeighinContent.class));
		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(
			competition,
			categories,
			groups,
			upload,
			athletes,
			weighIn);
		
		upload.setEnabled(false);
		
		fillH(grid, this);
	}
	
	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.BaseNavigationContent#configureTopBar(java.lang.String, com.github.appreciated.app.layout.behaviour.AppLayout)
	 */
	@Override
	protected void createTopBar (String title) {
		super.createTopBar("Prepare Competition");
	}

	
	@Override
	protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
		return null;
	}
	
	@Override
	protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
		return null;
	}

}
