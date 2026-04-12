/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Comment</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Comment#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Comment#getFilePath <em>File Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Comment#getStartLine <em>Start Line</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Comment#getEndLine <em>End Line</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Comment#getAuthor <em>Author</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Comment#getContent <em>Content</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Comment#getTimestamp <em>Timestamp</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Comment#isResolved <em>Resolved</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComment()
 * @model
 * @generated
 */
public interface Comment extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComment_Id()
	 * @model
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Comment#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>File Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>File Path</em>' attribute.
	 * @see #setFilePath(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComment_FilePath()
	 * @model
	 * @generated
	 */
	String getFilePath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Comment#getFilePath <em>File Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>File Path</em>' attribute.
	 * @see #getFilePath()
	 * @generated
	 */
	void setFilePath(String value);

	/**
	 * Returns the value of the '<em><b>Start Line</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Start Line</em>' attribute.
	 * @see #setStartLine(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComment_StartLine()
	 * @model
	 * @generated
	 */
	int getStartLine();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Comment#getStartLine <em>Start Line</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Start Line</em>' attribute.
	 * @see #getStartLine()
	 * @generated
	 */
	void setStartLine(int value);

	/**
	 * Returns the value of the '<em><b>End Line</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>End Line</em>' attribute.
	 * @see #setEndLine(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComment_EndLine()
	 * @model
	 * @generated
	 */
	int getEndLine();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Comment#getEndLine <em>End Line</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>End Line</em>' attribute.
	 * @see #getEndLine()
	 * @generated
	 */
	void setEndLine(int value);

	/**
	 * Returns the value of the '<em><b>Author</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Author</em>' attribute.
	 * @see #setAuthor(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComment_Author()
	 * @model
	 * @generated
	 */
	String getAuthor();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Comment#getAuthor <em>Author</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Author</em>' attribute.
	 * @see #getAuthor()
	 * @generated
	 */
	void setAuthor(String value);

	/**
	 * Returns the value of the '<em><b>Content</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Content</em>' attribute.
	 * @see #setContent(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComment_Content()
	 * @model
	 * @generated
	 */
	String getContent();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Comment#getContent <em>Content</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Content</em>' attribute.
	 * @see #getContent()
	 * @generated
	 */
	void setContent(String value);

	/**
	 * Returns the value of the '<em><b>Timestamp</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Timestamp</em>' attribute.
	 * @see #setTimestamp(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComment_Timestamp()
	 * @model
	 * @generated
	 */
	String getTimestamp();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Comment#getTimestamp <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Timestamp</em>' attribute.
	 * @see #getTimestamp()
	 * @generated
	 */
	void setTimestamp(String value);

	/**
	 * Returns the value of the '<em><b>Resolved</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Resolved</em>' attribute.
	 * @see #setResolved(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComment_Resolved()
	 * @model
	 * @generated
	 */
	boolean isResolved();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Comment#isResolved <em>Resolved</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Resolved</em>' attribute.
	 * @see #isResolved()
	 * @generated
	 */
	void setResolved(boolean value);

} // Comment
