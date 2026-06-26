ERROR com.io7m.ghrepostools.Main : The specified command does not exist.
  Command    : README
  Error Code : command-nonexistent

DEBUG com.io7m.ghrepostools.Main : Exception: 
com.io7m.quarrel.core.QException: The specified command does not exist.
	at com.io7m.quarrel.core.QApplication.parseExpanded(QApplication.java:244)
	at com.io7m.quarrel.core.QApplication.parse(QApplication.java:152)
	at com.io7m.quarrel.core.QApplicationType.run(QApplicationType.java:94)
	at com.io7m.ghrepostools.Main.run(Main.java:126)
	at com.io7m.ghrepostools.Main.mainExitless(Main.java:110)
	at com.io7m.ghrepostools.Main.main(Main.java:95)
