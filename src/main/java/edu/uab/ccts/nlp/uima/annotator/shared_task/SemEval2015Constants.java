package edu.uab.ccts.nlp.uima.annotator.shared_task;

import java.util.Hashtable;

import org.apache.uima.cas.CAS;

public class SemEval2015Constants
{

        public static final String APP_VIEW = "APPLICATION_VIEW";
        public static final String PIPED_VIEW = "PIPE_VIEW";
        public static final String GOLD_VIEW = CAS.NAME_DEFAULT_SOFA;

        public static final String OUTPUT_SEPERATOR = "|";
        public static final int TOTAL_FIELDS = 19; //22 in previous version

        public static final String NEGATION_RELATION = "Negation";
        public static final String SUBJECT_RELATION = "Subject";
        public static final String UNCERTAINITY_RELATION = "Uncertainty";
        public static final String COURSE_RELATION = "Course";
        public static final String SEVERITY_RELATION = "Severity";
        public static final String CONDITIONAL_RELATION = "Conditional";
        public static final String GENERIC_RELATION = "Generic";
        public static final String BODY_RELATION = "BodyLocation";
        public static final String DOCTIME_RELATION = "Doctime";
        public static final String TEMPORAL_RELATION = "Temporal";

        public static final Hashtable<String,String> defaultNorms = new Hashtable<String,String>();

        static {
                defaultNorms.put(NEGATION_RELATION, "no");
                defaultNorms.put(SUBJECT_RELATION, "patient");
                defaultNorms.put(UNCERTAINITY_RELATION, "no");
                defaultNorms.put(COURSE_RELATION, "unmarked");
                defaultNorms.put(SEVERITY_RELATION, "unmarked");
                defaultNorms.put(CONDITIONAL_RELATION, "false");
                defaultNorms.put(GENERIC_RELATION, "false");
                defaultNorms.put(BODY_RELATION, "null");
                defaultNorms.put(DOCTIME_RELATION, "unknown");
                defaultNorms.put(TEMPORAL_RELATION, "none");
        }



}
