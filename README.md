# owlcms4
Olympic Weightlifting Competition Management System 

Ongoing rewrite of [owlcms](https://owlcms2.sourceforge.io/#!index.md) using [Web Components](https://www.webcomponents.org/introduction) and [Vaadin Flow](https://vaadin.com/flow)

Two main incentives for this rewrite:
- owlcms2 shows its age (owlcms was initially written in 2009). Some of the underlying components cannot be upgraded, and as a consequence some bugs
cannot be fixed, leading to convoluted workarounds in the code.
- Being able to run in the cloud, with decisions, timers and sounds handled locally in the browser.  This should also reduce the need to run ethernet wiring - the new version will resync automatically if the connection is lost or the application is restarted.

Current status: Quite close to minimal viable product ("MVP") able to run a regional competition
- Working announcer, marshall and timekeeper screens (updating athlete cards and recomputing lifting order).
- Working attempt board and results board, with timing and decisions handled locally in the browser. USB/Bluetooth keypresses are processed directly in the browser for refereeing.
- Working Athlete Registration and Weigh-in screens, including producing weigh-in sheet
- Working forms for defining a competition (general info, groups, categories, etc.)
- Working protocol sheet with starting weights, working result sheet.
- A [live demo](https://owlcms4.herokuapp.com) of a recent build is normally available on the Heroku cloud service. Note that the cloud demo application is not pre-loaded and uses their free tier, so the first load can take a minute. This is *not* indicative of subsequent loads and is not indicative of local performance (both of which start in a few seconds).

Next steps
- Producing the lifter cards
- Athlete-facing option for decision-lights
- Upload of athlete registrations
- Technical Official names captured on group editing form
- Additional validations on weight request (athlete card)
- *Minimal* packaging and documentation for early users/testers

Design notes:
- The overall navigation and layout works using [vaadin-app-layout](https://github.com/appreciated/vaadin-app-layout)
- Administrative screen (e.g. categories) are built using [crudui](https://github.com/alejandro-du/crudui)
- Event-based design, strict separation between the presentation, the field-of-play business layer, and the back-end data
- Why is it called owlcms4? First there was owlcms. Did a major cleanup, and moved the code to sourceforge, owlcms2 was born. A few years back I started an owlcms3 rewrite, but it was too tedious to implement the off-line features I wanted, so I gave up until Vaadin Flow came out to rekindle my interest.
