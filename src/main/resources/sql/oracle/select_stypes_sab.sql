SELECT 
DISTINCT (mrconso.cui) as bcui,
last_value (mrconso.str) OVER  (partition by mrconso.cui ORDER BY mrrank.rank ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) AS best_text,
theabr, mrconso.sab
FROM mrconso JOIN mrrank ON (mrconso.tty=mrrank.tty AND mrconso.sab=mrrank.sab)
JOIN (
SELECT mrsty.cui,wm_concat(abr) as theabr  FROM umls.MRSTY JOIN umls.srdef
oN (umls.mrsty.tui= umls.srdef.ui) WHERE mrsty.cui= ? GROUP BY mrsty.cui
) abtable ON (abtable.cui=mrconso.cui)
WHERE mrconso.LAT='ENG' and mrconso.cui = ? and mrconso.sab < 'SNOMEDCU'
ORDER BY mrconso.sab DESC

