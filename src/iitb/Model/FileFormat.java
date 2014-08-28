package iitb.Model;



/**
 * Created by Maximian Rudolph on 28.08.14.
 */
public final class FileFormat {
  /**
 *
 * This constants parameterize the serialization format of the files written by FeatureGenImpl.
 * They can be tweaked if the given character is used in a name of the features.
 * As the files are processed linewise, '\n' is not allowed.
 * They are partly are processed recursively, so the WORD_*_SEPARATOR and FEATURE_*_SEPARATOR
 * musn't be the same.
 *
 * A feature-file has the following content:
 *
 * word_count(int) \n {1}
 * word(string) WORD_ELEMENT_SEPARATOR + pos(int) (WORD_ELEMENT_SEPARATOR class(int) WORD_STATE_SEPARATOR + count(int) )* \n {word_count}
 * "******* Features ************" \n {1}
 * feature_count(int) \n {1}
 * feature(entry) FEATURE_SEPARATOR pos(int) \n
 *
 *
 * a entry has the following structure:
 * name(string)  FEATURE_IDENTIFIER_SEPARATOR id(int) FEATURE_IDENTIFIER_SEPARATOR state_id(int)
 */

  static final String WORD_ELEMENT_SEPARATOR = " "; //separator between elements on a line

  static final String WORD_STATE_SEPARATOR = ":"; //separtor between class and count
  static final String FEATURE_SEPARATOR = " "; //separator a feature identifier from it's position
  static final String FEATURE_IDENTIFIER_SEPARATOR = ":"; //separtor between identifier properties

  private FileFormat() {
  }
}
